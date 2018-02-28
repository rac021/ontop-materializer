
package entry ;

import ontop.Manager ;
import org.slf4j.LoggerFactory ;
import ch.qos.logback.classic.Level ;
import ch.qos.logback.classic.Logger ;

public class Main_1_18 {

    /**
     * Main client program
     * @param args
     * @throws java.lang.Exception
     */
    
    public static void main(String[] args) throws Exception {
          
        final String defaultSparqlQuery =  "SELECT ?S ?P ?O { ?S ?P ?O . } " ;

        String owlFile     = ""  , obdaFile   = "" , outFile = "" , 
               sparqlQuery = ""  ;
        
        boolean  turtleOutFormat = false      ;
        boolean  not_out_onto    = false      ;
        boolean  out_onto        = false      ;
        
        boolean  debug           = false      ;
        boolean  existQuery      = false      ;
        boolean  batch           = false      ;
        int      pageSize        = -1         ;
        boolean  merge           = false      ;
        int      fragment        = 0          ;
        int      flushCount      = 10_0000    ;
       
        Level    level           = Level.OFF  ; 
        
        for ( int i = 0 ; i < args.length ; i++ )  {
            
            String token = args[i]     ;
           
            switch ( token ) {
                
              case "-owl"              : owlFile         = args[i+1]  ;
                                         break ;
              case "-obda"             : obdaFile        = args[i+1]  ;
                                         break ;
              case "-out"              : outFile         = args[i+1]  ;
                                         break ;
              case "-ttl"              : turtleOutFormat = true       ;
                                         break ;
              case "-q"                : sparqlQuery     = args[i+1]  ;
                                         existQuery      = true       ;
                                         break ;                   
              case "-not_out_ontology" : not_out_onto    = true       ;
                                         break ;                   
              case "-out_ontology"     : out_onto        = true       ;
                                         break ;                   
              case "-merge"            : merge           = true       ;
                                         break ;
              case "-batch"            : batch           = true       ;  
                                         break ;
              case "-debug"            : debug = true                 ;
                                         break ;
              case "-pageSize"         : pageSize        = 
                                         validate ( Integer.parseInt ( args[i+1] ) ) ;
                                         break ;
              case "-f"                : fragment   = Integer.parseInt ( args[i+1] ) ;
                                         break ;
              case "-flushCount"       : flushCount = Integer.parseInt ( args[i+1] ) ;
                                         break ;
              case "-log_level"        : level       = checkLog ( args[i+1] )        ;
                                         break ;
            }
        }
       
        if( not_out_onto )                  {
            Manager.output_ontology = false ;
        }
        else if( ! out_onto && ! batch )    {
           Manager.output_ontology = false  ;
        }
        else if( out_onto )                 {
           Manager.output_ontology = true   ;
        }
             
        System.out.println(" owl        =  " + owlFile         ) ;
        System.out.println(" obda       =  " + obdaFile        ) ;
        System.out.println(" out        =  " + outFile         ) ;
        System.out.println(" q          =  " + sparqlQuery     ) ;
        System.out.println(" TurtleOut  =  " + turtleOutFormat ) ;
        System.out.println(" Fragment   =  " + fragment        ) ;
        System.out.println(" -------------------------------- ") ;
        System.out.println(" Out_Onto   =  " + ! not_out_onto  ) ;
        System.out.println(" BATCH      =  " + batch           ) ;
        System.out.println(" MERGE      =  " + merge           ) ;
        System.out.println(" DEBUG_MODE =  " + debug           ) ;
        System.out.println(" -------------------------------- ") ;
        System.out.println("                                  ") ;
        
        
        if( level == Level.OFF )                   {
            ontop.Processor.HIDE_SYSTEM_OUT = true ;
        }
      
        System.out.println(" Applaying LOG-LEVEL : " + level ) ;
        createLoggerFor( level  )                              ;
        System.out.println( " " )                              ;
        
        if( owlFile.isEmpty() || obdaFile.isEmpty() || outFile.isEmpty()  ) {
           System.out.println( " " )                          ;
           System.out.println(" Missing some parameters !! ") ;
           System.out.println( " " )                          ;
           return                                             ;
        }
        
        if( ! existQuery )                          {
            sparqlQuery     = defaultSparqlQuery    ;
            turtleOutFormat = true                  ;
        }
       
        long startTime = System.currentTimeMillis() ;
      
           Manager.process( owlFile         ,
                            obdaFile        ,
                            sparqlQuery     , 
                            outFile         ,
                            turtleOutFormat ,
                            batch           ,
                            pageSize        ,
                            fragment        ,
                            merge           ,
                            flushCount      ,
                            debug         ) ;
        
        System.out.println(" ")                                                  ;
        long executionTime = System.currentTimeMillis() - startTime              ;
        System.out.println( " Elapsed seconds : " + executionTime / 1000 +" s" ) ; 
        System.out.println( " " )                                                ;
                   
    }

    private static int validate ( int pageSize ) {
       
        if( pageSize < 0 ) {
           System.out.println( " pageSize can't be Negatif !! " ) ;
           System.out.println( "                              " ) ;
           System.exit ( 0 )                                      ;
        }
        return pageSize ;
    }
    
    
    private static void createLoggerFor( Level level )  {
        
     Logger root = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
     root.setLevel(level);

    }

    private static Level checkLog (String level) {
     
        try {
             return  Level.toLevel(level.toUpperCase() )  ;
        } catch( Exception ex )  {
            System.out.println(" Error : The Level "
                               + " [" + level +"] deosn't exists."  ) ;
            System.out.println(" Retained LEVEL : OFF             " ) ;
            System.out.println("                                  " ) ;
             return Level.OFF                                         ;
        }

    }
    
}
