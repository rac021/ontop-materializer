
package entry ;

import ontop.Manager ;

public class Main_3 {
 
    /**
     * Main client program
     * @param args
     * @throws java.lang.Exception
     */
    
    public static void main(String[] args) throws Exception {
          
        final String defaultSparqlQuery =  "SELECT DISTINCT ?S ?P ?O { ?S ?P ?O . } " ;

        String owlFile     = ""  , obdaFile   = "" , outFile = "" , 
               sparqlQuery = ""  , connection = ""  ;
        
        boolean  turtleOutFormat = false   ;
        boolean  dev             = false   ;
        boolean  existQuery      = false   ;
        boolean  batch           = false   ;
        int      pageSize        = -1      ;
        boolean  merge           = false   ;
        int      fragment        = 0       ;
        int      flushCount      = 10_0000 ;
       
        
        for ( int i = 0 ; i < args.length ; i++ )  {
            
            String token = args[i]     ;
           
            switch ( token ) {
                
                    case "-owl"        : owlFile         = args[i+1]  ;
                                         break ;
                    case "-obda"       : obdaFile        = args[i+1]  ;
                                         break ;
                    case "-out"        : outFile         = args[i+1]  ;
                                         break ;
                    case "-ttl"        : turtleOutFormat = true       ;
                                         break ;
                    case "-q"          : sparqlQuery     = args[i+1]  ;
                                         existQuery      = true       ;
                                         break ;                   
                    case "-merge"      : merge           = true       ;
                                         break ;
                    case "-batch"      : batch           = true       ;  
                                         break ;
                    case "-dev"        : dev = true                   ;
                                         break ;
                    case "-pageSize"   : pageSize        = 
                                         validate ( Integer.parseInt ( args[i+1] ) ) ;
                                         break ;
                    case "-f"          : fragment   = Integer.parseInt ( args[i+1] ) ;
                                         break ;
                    case "-flushCount" : flushCount = Integer.parseInt ( args[i+1] ) ;
                                         break ;
                    case "-connection" : connection = args[i+1] ;
                                         break ;
            }
        }
       
        System.out.println(" owl        =  " + owlFile     )     ;
        System.out.println(" obda       =  " + obdaFile    )     ;
        System.out.println(" connection =  " + connection  )     ;
        System.out.println(" out        =  " + outFile     )     ;
        System.out.println(" q          =  " + sparqlQuery )     ;
        System.out.println(" TurtleOut  =  " + turtleOutFormat ) ;
        System.out.println(" Fragment   =  " + fragment        ) ;
        System.out.println("                           " )       ;
        
        if( owlFile.isEmpty() || obdaFile.isEmpty()  || 
            outFile.isEmpty() || connection.isEmpty() )    {
           System.out.println( " " )                       ;
           System.out.println(" Missing parameters !! ")   ;
           System.out.println( " " )                       ;
           return                                          ;
        }
        
        if( !existQuery ) {
            sparqlQuery         = defaultSparqlQuery    ;
            turtleOutFormat = true                      ;
        }
       
        long startTime = System.currentTimeMillis()     ;
      
           Manager.process( owlFile         ,
                            obdaFile        ,
                            connection      ,
                            sparqlQuery     , 
                            outFile         ,
                            turtleOutFormat ,
                            batch           ,
                            pageSize        ,
                            fragment        ,
                            merge           ,
                            flushCount      ,
                            dev           ) ;
        
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
}
