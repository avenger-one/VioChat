package space.deniska;

import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class VioChat extends JavaPlugin
{

    private static String AdminSymbol = "@";
    private static String GlobalSymbol = "!";
    private static Chat chat = null;

    @Override
    public void onEnable( )
    {
        getLogger( ).info( "plugin initializated" );
        SettingsManager.getInstance( ).setup( this );

        RegisteredServiceProvider< Chat > rsp = getServer( ).getServicesManager( ).getRegistration( Chat.class );
        chat = rsp.getProvider( );

        AdminSymbol = SettingsManager.getInstance( ).getConfig( ).getString( "Admin.Symbol" );
        GlobalSymbol = SettingsManager.getInstance( ).getConfig( ).getString( "Global.Symbol" );

        Bukkit.getPluginManager( ).registerEvents( new Listener( )
        {
            @EventHandler
            public void onChatEvent( AsyncPlayerChatEvent e )
            {
                int m_iChatType = 0;

                Player m_Recipient = e.getPlayer( );

                String raw = e.getMessage( );
                String template;
                String m_szColor = SettingsManager.getInstance( ).getConfig( ).getString( "Local.Color" );

                if ( raw.startsWith( GlobalSymbol ) )
                {
                    template = "Global.Template";
                    m_szColor = SettingsManager.getInstance( ).getConfig( ).getString( "Global.Color" );
                    raw = raw.substring( 1 );
                    m_iChatType = 1;
                }
                else if ( raw.startsWith( AdminSymbol ) && e.getPlayer().hasPermission( "viochat.admin" ) )
                {
                    template = "Admin.Template";
                    m_szColor = SettingsManager.getInstance( ).getConfig( ).getString( "Admin.Color" );
                    raw = raw.substring( 1 );
                    m_iChatType = 2;
                }
                else
                    template = "Local.Template";

                String msg = SettingsManager.getInstance( ).getConfig( ).getString( template );
                m_szColor = m_szColor.replace( "&", "§" );

                if ( raw.startsWith( " " ) )
                    raw = raw.substring( 1 );

                for ( Player tempPlayer : e.getRecipients( ) )
                {
                    if ( raw.startsWith( tempPlayer.getDisplayName( ) + SettingsManager.getInstance( ).getConfig( ).getString( "Private.Symbol" ) ) && m_iChatType == 0 )
                    {
                        if ( tempPlayer == e.getPlayer( ) )
                            continue;

                        m_Recipient = tempPlayer;
                        m_iChatType = 3;

                        msg = SettingsManager.getInstance( ).getConfig( ).getString( "Private.Template" );
                        m_szColor = SettingsManager.getInstance( ).getConfig( ).getString( "Private.Color" ).replace( "&", "§" );

                        raw = raw.replace( tempPlayer.getDisplayName( ) + SettingsManager.getInstance( ).getConfig( ).getString( "Private.Symbol" ), "" );

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

                int distance = 100;
                Location pLoc = e.getPlayer( ).getLocation( );

                for ( Player pl : e.getRecipients( ) )
                {

                    if ( ( pl.getLocation( ).distance( pLoc ) <= distance && m_iChatType == 0 )
                            || ( m_iChatType == 1 )
                            || ( m_iChatType == 2 && pl.hasPermission( "viochat.admin" ) )
                            || ( m_iChatType == 3 && ( pl == m_Recipient || pl == e.getPlayer( ) ) ) )
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
