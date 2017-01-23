
package ontop;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ryahiaoui
 */
public class SqlManager {

    private final String driver   ;
    private final String url      ;
    private final String username ;
    private final String password ;

    private final Connection connection ;
    
    public SqlManager( String driver   ,
                      String url      ,
                      String username ,
                      String password ) throws Exception {
    
        this.driver   = driver   ;
        this.url      = url      ;
        this.username = username ;
        this.password = password ;

        Class.forName(driver)    ;
        Connection conn = DriverManager.getConnection( url, username, password) ;
        this.connection = conn ;

    }
    
    
    public List<String> extractHeader( String sql ) throws Exception {

      PreparedStatement ps       = connection.prepareStatement(sql) ;
      ResultSetMetaData metaData = ps.getMetaData()                 ;
        
      List<String> headers = new ArrayList() ;
      
       for (int i = 1; i <= metaData.getColumnCount() ; i++) {
          headers.add( metaData.getColumnLabel(i) ) ;
       }

      return headers ;
      
    }
    
    public String getDbProvider() {
        if( driver.contains("postgresql")) return "postgresql" ;
        if( driver.contains("mysql"))      return "mysql"      ;
        return null ;
    }
}
