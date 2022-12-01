package space.deniska;

import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class VioChat extends JavaPlugin
{

    private static String AdminSymbol = "@";
    private static String GlobalSymbol = "!";
    private static String RPSymbol = "*";
    private static Chat chat = null;
    private static boolean g_bChatBlocked = false;

    @Override
    public void onEnable( )
    {
        getLogger( ).info( "plugin initializated" );
        SettingsManager.getInstance( ).setup( this );

        RegisteredServiceProvider< Chat > rsp = getServer( ).getServicesManager( ).getRegistration( Chat.class );
        chat = rsp.getProvider( );

        AdminSymbol = SettingsManager.getInstance( ).getConfig( ).getString( "Chats.Admin.Symbol" );
        GlobalSymbol = SettingsManager.getInstance( ).getConfig( ).getString( "Chats.Global.Symbol" );
        RPSymbol = SettingsManager.getInstance( ).getConfig( ).getString( "Chats.Roleplay.Symbol" );

        getCommand( "chatblock" ).setExecutor( ( commandSender, command, s, args ) ->
        {
            if ( !commandSender.hasPermission( "viochat.admin" ) )
            {
                commandSender.sendMessage(ChatColor.RED + "Нет прав");
                return true;
            }

            if ( args.length != 0 )
            {
                commandSender.sendMessage( ChatColor.RED + "Неверное количество аргументов" );
                return true;
            }

            g_bChatBlocked = !g_bChatBlocked;
            commandSender.sendMessage( ChatColor.GREEN + ( ( g_bChatBlocked ) ? "Чат заблокирован" : "Чат разблокирован" ) );

            return true;
        });

        Bukkit.getPluginManager( ).registerEvents( new Listener( )
        {
            @EventHandler
            public void onChatEvent( AsyncPlayerChatEvent e )
            {
                int iChatType = 0;

                Player m_Recipient = e.getPlayer( );

                String raw = e.getMessage( );
                String template;
                String m_szColor = SettingsManager.getInstance( ).getConfig( ).getString( "Chats.Local.Color" );

                if ( g_bChatBlocked && !e.getPlayer( ).hasPermission( "viochat.admin" ) )
                {
                    e.getPlayer( ).sendMessage( ChatColor.RED + "Чат заблокирован. Возможно, на сервере проводится ивент!" );
                    e.setCancelled( true );
                    return;
                }

                if ( raw.startsWith( GlobalSymbol ) )
                {
                    template = "Chats.Global.Template";
                    m_szColor = SettingsManager.getInstance( ).getConfig( ).getString( "Chats.Global.Color" );
                    raw = raw.substring( GlobalSymbol.length( ) );
                    iChatType = 1;
                }
                else if ( raw.startsWith( AdminSymbol ) && e.getPlayer().hasPermission( "viochat.admin" ) )
                {
                    template = "Chats.Admin.Template";
                    m_szColor = SettingsManager.getInstance( ).getConfig( ).getString( "Chats.Admin.Color" );
                    raw = raw.substring( AdminSymbol.length( ) );
                    iChatType = 2;
                }
                else if ( raw.startsWith( RPSymbol ) )
                {
                    template = "Chats.Roleplay.Template";
                    m_szColor = SettingsManager.getInstance( ).getConfig( ).getString( "Chats.Roleplay.Color" );
                    iChatType = 4;
                    raw = raw.substring( RPSymbol.length( ) );
                }
                else
                {
                    template = "Chats.Local.Template";
                }

                String msg = SettingsManager.getInstance( ).getConfig( ).getString( template );
                m_szColor = m_szColor.replace( "&", "§" );

                if ( raw.startsWith( " " ) )
                    raw = raw.substring( 1 );

                for ( Player tempPlayer : e.getRecipients( ) )
                {
                    if ( raw.startsWith( tempPlayer.getDisplayName( ) + SettingsManager.getInstance( ).getConfig( ).getString( "Chats.Private.Symbol" ) ) && iChatType == 0 )
                    {
                        if ( tempPlayer == e.getPlayer( ) )
                            continue;

                        m_Recipient = tempPlayer;
                        iChatType = 3;

                        msg = SettingsManager.getInstance( ).getConfig( ).getString( "Chats.Private.Template" );
                        m_szColor = SettingsManager.getInstance( ).getConfig( ).getString( "Chats.Private.Color" ).replace( "&", "§" );

                        raw = raw.replace( tempPlayer.getDisplayName( ) + SettingsManager.getInstance( ).getConfig( ).getString( "Chats.Private.Symbol" ), "" );

                        if ( raw.startsWith( " " ) )
                            raw = raw.substring( 1 );
                    }

                    String modPlayer = chat.getPlayerPrefix( tempPlayer ) + tempPlayer.getDisplayName( ) + chat.getPlayerSuffix( tempPlayer ) + m_szColor;

                    if ( ( raw.startsWith( tempPlayer.getDisplayName( ) + " " )
                            || raw.endsWith( " " + tempPlayer.getDisplayName( ) )
                            || raw.contains( " " + tempPlayer.getDisplayName( ) + " " ) )
                            || raw.equalsIgnoreCase( tempPlayer.getDisplayName( ) ) )
                        raw = raw.replace( tempPlayer.getDisplayName( ), modPlayer );
                }

                msg = msg.replace( "%username%", e.getPlayer( ).getDisplayName( ) );
                msg = msg.replace( "%receiver%", chat.getPlayerPrefix( m_Recipient ) + m_Recipient.getDisplayName( ) + chat.getPlayerSuffix( m_Recipient ) );
                msg = msg.replace( "%message%", raw );
                msg = msg.replace( "%prefix%", chat.getPlayerPrefix( e.getPlayer( ) ) );
                msg = msg.replace( "%suffix%", chat.getPlayerSuffix( e.getPlayer( ) ) );
                msg = msg.replace( "&", "§" );

                Location pLoc = e.getPlayer( ).getLocation( );

                int m_iDistance = SettingsManager.getInstance( ).getConfig( ).getInt( "Chats." + ( ( iChatType == 4 ) ? "Roleplay" : "Local" ) + ".Distance" );

                for ( Player pl : e.getRecipients( ) )
                {
                    if ( pl.getLocation( ).getWorld( ) != pLoc.getWorld( ) && ( iChatType == 0 || iChatType == 4 ) )
                        continue;

                    if ( ( pl.getLocation( ).distance( pLoc ) <= m_iDistance && ( iChatType == 0 || iChatType == 4 ) )
                            || ( iChatType == 1 )
                            || ( iChatType == 2 && pl.hasPermission( "viochat.admin" ) )
                            || ( iChatType == 3 && ( pl == m_Recipient || pl == e.getPlayer( ) ) ) )
                    {
                        pl.sendMessage( msg );
                    }

                }

                e.getRecipients( ).clear( );
            }

        }, this );
    }

    @Override
    public void onDisable( )
    {
        getLogger( ).info( "Bye! Don't forget to credit violanes" );
    }

}
