package space.deniska;

import java.io.File;
import java.io.IOException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public class SettingsManager
{

    private SettingsManager( ) {}

    static SettingsManager instance = new SettingsManager( );

    public static SettingsManager getInstance( )
    {
        return instance;
    }

    private Plugin p;
    private FileConfiguration config;
    private File cfile;

    public void setup( Plugin p )
    {
        this.p = p;

        cfile = new File( p.getDataFolder( ), "config.yml" );

        if ( !cfile.exists( ) )
        {
            p.saveResource( "config.yml", false );
        }

        config = YamlConfiguration.loadConfiguration( cfile );
    }

    public FileConfiguration getConfig( )
    {
        return config;
    }

    public void saveConfig( )
    {
        try
        {
            config.save( cfile );
        }
        catch ( IOException e )
        {
            e.printStackTrace( );
        }
    }

    public void reloadConfig( )
    {
        config = YamlConfiguration.loadConfiguration( cfile );
    }
}