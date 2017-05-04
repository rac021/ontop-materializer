
package ontop.entity;

import static java.text.MessageFormat.format;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author ryahiaoui
 */
public class Mapping {
    
    public static       String header        ;
    public static final String footer = "]]" ;
    
    
    private final String  mappingKey =  "mappingId \t " ;
    private final String  targetKey  =  "target \t\t"   ;
    private final String  sourceKey  =  "source	\t "    ;
    private final String  newLine    =  "\n"            ;
    

    private final String id            ;
    private final String target        ;
    private final String query         ;
    private       String templateQuery ;
    private       String instanceQuery ;

    private       int OFFSET     =  0  ;
    public static int PAGE_SIZE  =  0  ;
    
    public static String FILE_NAME           ;
    public static String EXTENSION = ".ttl"  ;
   
    
    public Mapping( String id     , 
                    String target , 
                    String query  ) {
        
        this.id            = id                       ;
        this.target        = target                   ;
        this.query         = cleanQuery(query.trim()) ;
        this.templateQuery = this.query               ;
        this.instanceQuery = this.query               ;
    }

    private String cleanQuery( String query )       {
      return query.endsWith(";")                    ? 
             query.replaceAll(" +", " ").trim()
                  .substring( 0, query.length()-1 ) :
             query.replaceAll(" +", " ").trim()     ;
    }
    
    public static String getHeader() {
        return header;
    }

    public static String getFooter() {
        return footer;
    }

    public String getId() {
        return id;
    }
    
    public String getCleanedId() {
        return id.replace("(", "").replace(")", "").replace("/", "-") ;
    }

    public String getTarget() {
        return target;
    }

    public String getQuery() {
        return query ;
    }
    
    public void applyParams ( List<String> headers , int PAGE_SIZE , String driver ) {
      
        if( PAGE_SIZE >= 0 ) {
            
          if( driver.equalsIgnoreCase("POSTGRESQL")) {

            Pattern p = Pattern.compile("LIMIT +.?\\d+.*", Pattern.CASE_INSENSITIVE) ;
            Matcher m = p.matcher( query ) ;

            String limit = "" , offset = "" ;

             if(m.find()) {
                 limit = m.group() ;
             }

            p = Pattern.compile("OFFSET +.?\\d+ ", Pattern.CASE_INSENSITIVE ) ;
            m = p.matcher( query ) ;

             if(m.find()) {
                 offset = m.group() ;
             }
              
             
              if( ! query.toLowerCase().contains("order by "))                 {
              
                   templateQuery += " ORDER BY " + String.join(", ", headers ) ;
              }
              
              if( offset.isEmpty() ) {
                 templateQuery += " OFFSET {0} " ;
              }
              else {
                 templateQuery = templateQuery.replace( offset, " " ) + offset ;
              }
              
              /* LIMIT MUST BE AT THE END : Because of Ontop Bug ! */
              
              if( limit.isEmpty() ) {
                 templateQuery += " LIMIT " + PAGE_SIZE ;
              }
              else {
                 templateQuery =  templateQuery.replace( limit, " " )  + limit ;
              }
              
              templateQuery = templateQuery.replaceAll(" +", " ").trim() + " " ;
          }

          else if( driver.equalsIgnoreCase("MYSQL")) {
              // toDo List
          }
      }
    }
    
    public String applyOffset( Integer OFFSET )            { 
        
        this.OFFSET = OFFSET                               ;
        instanceQuery = templateQuery.replaceAll("'","<>") ;        
        return instanceQuery = format( instanceQuery , 
                                       OFFSET.toString() ).replaceAll("<>", "'") ;
    }
        
    @Override
    public String toString() {
       return  header                     +  
               mappingKey + id            + newLine + 
               targetKey  + target        + newLine + 
               sourceKey  + instanceQuery + newLine + newLine + 
               footer                     ;
    }
  
    public String emptyMapping() {
       return  header               +  
               newLine + newLine    + 
               footer               ;
    }

    public int getOFFSET() {
        return OFFSET ;
    }

    public String getCodeFromId() {
       return  id.replaceAll("[^0-9]", "") ;
    }
}
