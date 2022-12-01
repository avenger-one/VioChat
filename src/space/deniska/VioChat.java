package space.deniska;

import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Iterator;

public class VioChat extends JavaPlugin
{
    private static String AdminSymbol = "@";
    private static String GlobalSymbol = "!";
    private static String RPSymbol = "*";
    private static boolean g_bChatBlocked = false;
    static Chat m_Chat = null;

    @Override
    public void onEnable( )
    {
        getLogger( ).info( "plugin initializated" );
        SettingsManager.getInstance( ).setup( this );

        RegisteredServiceProvider< Chat > rsp = getServer( ).getServicesManager( ).getRegistration( Chat.class );
        m_Chat = rsp.getProvider( );

        AdminSymbol = SettingsManager.getInstance( ).getConfig( ).getString( "Chats.Admin.Symbol" );
        GlobalSymbol = SettingsManager.getInstance( ).getConfig( ).getString( "Chats.Global.Symbol" );
        RPSymbol = SettingsManager.getInstance( ).getConfig( ).getString( "Chats.Roleplay.Symbol" );

        getCommand( "chatblock" ).setExecutor( ( commandSender, command, s, args ) ->
        {
            if ( !commandSender.hasPermission( "viochat.admin" ) )
            {
                commandSender.sendMessage( ChatColor.RED + "No permission" );
                return true;
            }

            if ( args.length != 0 )
            {
                commandSender.sendMessage( ChatColor.RED + "Invalid arguments" );
                return true;
            }

            g_bChatBlocked = !g_bChatBlocked;
            commandSender.sendMessage( ChatColor.GREEN + ( ( g_bChatBlocked ) ? "Chat was locked"
                : "Chat was unlocked" ) );

            return true;
        } );

        Bukkit.getPluginManager( ).registerEvents( new Listener( )
        {
            @EventHandler( priority = EventPriority.HIGHEST )
            public void onPlayerChat( AsyncPlayerChatEvent pEvent )
            {
                if ( pEvent.isCancelled( ) )
                    return;

                final Player pActor = pEvent.getPlayer( );
                if ( pActor == null )
                {
                    pEvent.setCancelled( true );
                    return;
                }

                String sRaw = pEvent.getMessage( );
                if ( sRaw.startsWith( " " ) )
                    sRaw = sRaw.substring( 1 );

                String sColor = SettingsManager.getInstance( ).getConfig( ).getString( "Chats.Local.Color" );
                String sTemplate;
                int iChatMode = 0;

                if ( sRaw.startsWith( GlobalSymbol ) )
                {
                    sTemplate = "Chats.Global.Template";
                    sColor = SettingsManager.getInstance( ).getConfig( ).getString( "Chats.Global.Color" );
                    sRaw = sRaw.substring( GlobalSymbol.length( ) );
                    iChatMode = 1;
                }
                else if ( sRaw.startsWith( AdminSymbol ) && pActor.hasPermission( "viochat.admin" ) )
                {
                    sTemplate = "Chats.Admin.Template";
                    sColor = SettingsManager.getInstance( ).getConfig( ).getString( "Chats.Admin.Color" );
                    sRaw = sRaw.substring( AdminSymbol.length( ) );
                    iChatMode = 2;
                }
                else if ( sRaw.startsWith( RPSymbol ) )
                {
                    sTemplate = "Chats.Roleplay.Template";
                    sColor = SettingsManager.getInstance( ).getConfig( ).getString( "Chats.Roleplay.Color" );
                    iChatMode = 4;
                    sRaw = sRaw.substring( RPSymbol.length( ) );
                }
                else
                {
                    sTemplate = "Chats.Local.Template";
                }

                String sMessage = SettingsManager.getInstance( ).getConfig( ).getString( sTemplate );

                Player pRecipient = pActor;
                for ( Player pIteratedPlayer : pEvent.getRecipients( ) )
                {
                    if ( sRaw.startsWith( pIteratedPlayer.getDisplayName( ) + SettingsManager.getInstance( ).getConfig( ).getString( "Chats.Private.Symbol" ) )
                            && iChatMode == 0 )
                    {
                        if ( pIteratedPlayer == pEvent.getPlayer( ) )
                            continue;

                        pRecipient = pIteratedPlayer;
                        iChatMode = 3;

                        sMessage = SettingsManager.getInstance( ).getConfig( ).getString( "Chats.Private.Template" );
                        sColor = SettingsManager.getInstance( ).getConfig( ).getString( "Chats.Private.Color" ).replace( "&", "§" );

                        sRaw = sRaw.replace( pIteratedPlayer.getDisplayName( ) + SettingsManager.getInstance( ).getConfig( ).getString( "Chats.Private.Symbol" ), "" );
                        if ( sRaw.startsWith( " " ) )
                            sRaw = sRaw.substring( 1 );
                    }

                    String sMention = m_Chat.getPlayerPrefix( pIteratedPlayer ) + pIteratedPlayer.getDisplayName( ) + m_Chat.getPlayerSuffix( pIteratedPlayer ) + sColor;
                    if ( ( sRaw.startsWith( pIteratedPlayer.getDisplayName( ) + " " )
                            || sRaw.endsWith( " " + pIteratedPlayer.getDisplayName( ) )
                            || sRaw.contains( " " + pIteratedPlayer.getDisplayName( ) + " " ) )
                            || sRaw.equalsIgnoreCase( pIteratedPlayer.getDisplayName( ) ) )
                    {
                        sRaw = sRaw.replace( pIteratedPlayer.getDisplayName( ), sMention );
                    }
                }

                while ( sRaw.startsWith( " " ) )
                    sRaw = sRaw.substring( 1 );

                if ( !pActor.hasPermission( "viochat.color" ) )
                    sRaw = sRaw.replaceAll( "&.", "" );

                sMessage = sMessage.replace( "%username%", pActor.getDisplayName( ) );
                sMessage = sMessage.replace( "%receiver%", m_Chat.getPlayerPrefix( pRecipient )
                        + pRecipient.getDisplayName( ) + m_Chat.getPlayerSuffix( pRecipient ) );
                sMessage = sMessage.replace( "%message%", sRaw );
                sMessage = sMessage.replace( "%prefix%", m_Chat.getPlayerPrefix( pActor ) );
                sMessage = sMessage.replace( "%suffix%", m_Chat.getPlayerSuffix( pActor ) );
                sMessage = sMessage.replace( "&", "§" );

                pEvent.setFormat( sMessage );

                if ( iChatMode != 0 && iChatMode != 4 )
                    return;

                final World LocalWorld = pActor.getLocation( ).getWorld( );
                final Iterator< Player > aRecipients = pEvent.getRecipients( ).iterator( );

                int iDistance = SettingsManager.getInstance( ).getConfig( ).getInt( "Chats." + ( ( iChatMode == 4 )
                        ? "Roleplay" : "Local" ) + ".Distance" );

                while ( aRecipients.hasNext( ) )
                {
                    final Player pIteratedPlayer = aRecipients.next( );
                    if ( pIteratedPlayer == pActor )
                        continue;

                    if ( pIteratedPlayer.getLocation( ).getWorld( ) == LocalWorld
                            && pActor.getLocation( ).distance( pIteratedPlayer.getLocation( ) ) <= iDistance )
                        continue;

                    aRecipients.remove( );
                }
            }
        }, this );
    }

    @Override
    public void onDisable( )
    {
        getLogger( ).info( "Bye! Don't forget to credit violanes" );
    }

}
