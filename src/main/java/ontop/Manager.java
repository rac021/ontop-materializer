
package ontop ;

import java.util.List ;
import java.util.ArrayList ;
import ontop.entity.Mapping ;
import java.util.regex.Matcher ;
import java.util.regex.Pattern ;
import ontop.entity.ResultInfo ;
import java.util.stream.Collectors ;

/**
 *
 * @author ryahiaoui
 */
 public class Manager {
    
  private static int     totalTriplesInOntology = 0    ;

  public static boolean  output_ontology        = true ;
    
  public static void process ( String  owlFile         ,
                               String  obdaFile        ,
                               String  connectionFile  ,
                               String  sparqlQuery     ,
                               String  outFile         ,
                               boolean turtleOutFormat ,
                               boolean batch           ,
                               int     pageSize        ,
                               int     fragment        ,
                               boolean merge           ,
                               int     flushCount      ,
                               boolean debug           )    throws Exception  {
      
    if( ! InOut.existFile(owlFile) ) {
        System.out.println( " " )                           ;
        System.out.println(" OWL File not found ! " )       ;
        System.out.println(" Program will be terminated " ) ;
        System.out.println( " " )                           ;
        return ;
    }
    if( ! InOut.existFile(obdaFile) ) {
        System.out.println( " " )                           ;
        System.out.println(" OBDA File not found ! " )      ;
        System.out.println(" Program will be terminated " ) ;
        System.out.println( " " )                           ;
        return ;
    }
    
    System.out.println(" ")   ;
    
    if( ! batch ) {
        
        /* BONUS */
        
        if( output_ontology ) {
       
            /* Transform File to String */
            String stringObdaFile = InOut.readTextFile(obdaFile )
                                         .stream()
                                         .collect (Collectors.joining ("\n")) ;

            /* Transform File to String */
            Mapping.connectionHeader  = InOut.readTextFile(connectionFile )
                                             .stream()
                                             .collect (Collectors.joining ("\n")) ;

            /* Transform File to String */
            Mapping.prefixDeclaration  = extractPrefixeclaration(stringObdaFile) ;

            /* extract Header from obda File */
            String fileName  = InOut.getfileName ( obdaFile )             ;

            Mapping.PAGE_SIZE =  pageSize                                 ;
            Mapping.FILE_NAME =  InOut.getFileWithoutExtension(fileName ) ;

            /*   Create tmp folder for sub-mappings */
            String subFolder          = InOut.extractFolder( obdaFile ) + "subMappings/" ;
            String subOBDAFilePath    = subFolder + "mapping.obda"                       ;
            InOut.deleteAndCreateFile   ( subOBDAFilePath    )                           ;

            String outOntology        = InOut.getFolder ( outFile ) + "/Ontology.ttl"    ;

            Mapping emptyMapping      = new Mapping( "0" , "" , "")                      ;

            String  ontoObdaFileName  = subFolder + "emptyMapping.obda"                  ;

            /* Obda File */
            InOut.writeTextFile( emptyMapping.emptyMapping(), ontoObdaFileName  )        ;

            System.out.println(" Process Ontology ..." )                   ;
       
            totalTriplesInOntology = processMappingOnto( ontoObdaFileName  ,
                                                         owlFile           ,
                                                         connectionFile    ,
                                                         outOntology       ,
                                                         sparqlQuery       ,
                                                         turtleOutFormat   ,
                                                         fragment          ,
                                                         flushCount        ,
                                                         debug             ,
                                                         output_ontology 
                                                       ).getTotal()        ;
        }
       
        System.out.println(" Start Data Generation  --> "
                         + " [ DISABLED BATCH MODE ] " )                   ;
        System.out.println(" This may take a while. Please wait ..     " ) ;
        System.out.println("                                           " ) ;

        ResultInfo res = new Processor ( owlFile  , 
                                         obdaFile , 
                                         connectionFile 
                                       ) .run( sparqlQuery     , 
                                               outFile         ,
                                               turtleOutFormat ,
                                               fragment        ,
                                               flushCount      ,
                                               debug         ) ;
        
       if( ! res.getGeneratedFiles().isEmpty() ) {
           
            res.getGeneratedFiles().forEach( out -> {
                System.out.println("  -> Generated File : " + out ) ;
            }) ;
       }
       
       return ;
    }
    
    /* Transform File to String */
    String stringObdaFile = InOut.readTextFile(obdaFile )
                                 .stream()
                                 .collect (Collectors.joining ("\n")) ;
    
    /* Transform File to String */
    Mapping.connectionHeader  = InOut.readTextFile(connectionFile )
                                     .stream()
                                     .collect (Collectors.joining ("\n")) ;
 
    /* Transform File to String */
    Mapping.prefixDeclaration  = extractPrefixeclaration(stringObdaFile) ;
    
    /* extract Header from obda File */
    String fileName  = InOut.getfileName ( obdaFile )             ;

    Mapping.PAGE_SIZE =  pageSize                                 ;
    Mapping.FILE_NAME =  InOut.getFileWithoutExtension(fileName ) ;
                 
    List<Mapping> mappings = extractMappings(stringObdaFile) ;
   
    SqlManager sqlManager = new SqlManager( extractDriver( Mapping.connectionHeader )     ,
                                            extractUrl( Mapping.connectionHeader )        ,
                                            extractUserName( Mapping.connectionHeader )   ,
                                            extractPassword( Mapping.connectionHeader ) ) ;
    
    /* Create tmp folder for sub-mappings */
    String subFolder          = InOut.extractFolder( obdaFile ) + "subMappings/" ;
    String subOBDAFilePath    = subFolder + "mapping.obda"                       ;
    InOut.deleteAndCreateFile ( subOBDAFilePath    )                             ;
    
    int currentPage  = 0  ; 
    int index        = 1  ; 
    
    if( ! mappings.isEmpty() ) {
       
        String outOntology       = InOut.getFolder ( outFile ) + "/Ontology.ttl"  ;

        Mapping emptyMapping     = new Mapping( "0" , "" , "")                    ;

        String  ontoObdaFileName = subFolder + "emptyMapping.obda"                ;

        /* Obda File */
        InOut.writeTextFile( emptyMapping.emptyMapping(), ontoObdaFileName  )     ;        

        if( output_ontology ) {
           System.out.println(" Process Ontology ..." )                ;
        }
        
        totalTriplesInOntology = processMappingOnto( ontoObdaFileName  ,
                                                     owlFile           ,
                                                     connectionFile    ,
                                                     outOntology       ,
                                                     sparqlQuery       ,
                                                     turtleOutFormat   ,
                                                     fragment          ,
                                                     flushCount        ,
                                                     debug             ,
                                                     output_ontology ).getTotal() ;
    }
 
    if( ! merge )  {
        owlFile =  subFolder + "emptyOwl.owl"   ;
        InOut.writeTextFile( "" , owlFile  )    ;
        totalTriplesInOntology  =  0            ;
    }
    
    System.out.println(" Start Data Generation  --> "
                     + " [ ENABLED BATCH MODE ] "   )                  ;
    System.out.println(" This may take a while. Please wait ..     " ) ;
    System.out.println("                                           " ) ;
       
    for( Mapping mapping : mappings )  {
        
        if( pageSize >= 0 ) {
            
          System.out.println(" Process Node : " + mapping.getCodeFromId() + " ..." ) ;
          
          /* Apply Params for current Sub Mapping */ 
          List<String> columns = sqlManager.extractColumns(mapping.getQuery() ) ;
          mapping.applyParams(columns, pageSize , sqlManager.getDbProvider() )  ;

          currentPage = 0                      ;
          mapping.applyOffset( currentPage ++) ;
          InOut.writeTextFile( mapping.toString(), subOBDAFilePath  ) ;

            
          while( processMapping ( mapping                ,
                                  subOBDAFilePath        ,
                                  owlFile                ,
                                  connectionFile         ,
                                  outFile                ,
                                  index   ++             ,
                                  sparqlQuery            ,
                                  turtleOutFormat        ,
                                  fragment               ,
                                  flushCount             ,
                                  debug                  ,
                                  merge                  ,
                                  true                  ).getTotal()
                                  - totalTriplesInOntology > 0 )   {

              /* Obda File */

              mapping.applyOffset(  ( ++currentPage -1 ) * pageSize   )   ;
              InOut.writeTextFile( mapping.toString(), subOBDAFilePath  ) ;
           }
          
           System.out.println("  ")                                       ;
        }
        else {
            
             System.out.println(" Process Node : " + mapping.getCodeFromId() + " ..." ) ;
             
             processMapping ( mapping            ,
                              subOBDAFilePath    ,
                              owlFile            ,
                              connectionFile     ,
                              outFile            ,
                              index   ++         ,
                              sparqlQuery        ,
                              turtleOutFormat    ,
                              fragment           ,
                              flushCount         ,
                              debug              ,
                              merge              ,
                              true             ) ; 
             
               
             System.out.println("  ")            ;
            
        }
    }
    
  }
  
  private static ResultInfo processMappingOnto ( String  subFileObda     , 
                                                 String  owlFile         ,
                                                 String  connectionFile  ,
                                                 String  outFiles        ,
                                                 String  sparqlQuery     ,
                                                 boolean turtleOutFormat ,
                                                 int     fragment        ,
                                                 int     flushCount      ,
                                                 boolean debug           ,
                                                 boolean output_ontology ) throws Exception {
 
    /* Process extraction */
     
    Processor ontop =  new Processor ( owlFile, subFileObda ,  connectionFile ) ;
        
    ResultInfo res = ontop.run ( sparqlQuery     , 
                                 outFiles        ,
                                 turtleOutFormat ,
                                 fragment        ,
                                 flushCount      ,
                                 debug         ) ;
   
    if( ! output_ontology )  {
       InOut.removeFiles( res.getGeneratedFiles() ) ;
    } else {
        
        if( output_ontology ) {
            res.getGeneratedFiles().forEach( out -> {
              System.out.println("  -> Generated File : " + out ) ;
            }) ;
        }
    }
        
    return res ;
  }
 
  
  private static ResultInfo processMapping ( Mapping mapping         ,
                                             String  subFileObda     , 
                                             String  owlFile         ,
                                             String  connectionFile  ,
                                             String  outFiles        ,
                                             int     index           ,
                                             String  sparqlQuery     ,
                                             boolean turtleOutFormat ,
                                             int     fragment        ,
                                             int     flushCount      ,
                                             boolean debug           ,
                                             boolean merge           ,
                                             boolean display_messg   ) throws Exception {
 
    /* out path result */
    String out =  outFiles                           + 
                  "_" + Mapping.FILE_NAME            +
                  "_code_" + mapping.getCodeFromId() +
                  "_LOF_"  + Mapping.PAGE_SIZE       +
                  "_"      + mapping.getOFFSET()     + 
                  "_idx_"  + index ++                +
                  Mapping.EXTENSION                  ;

    /* Process extraction */
   
    Processor ontop =  new Processor ( owlFile, subFileObda ,  connectionFile) ;
        
      ResultInfo res = ontop.run ( sparqlQuery     , 
                                   out             ,
                                   turtleOutFormat ,
                                   fragment        ,
                                   flushCount      ,
                                   debug         ) ;

    /* if  RES = totalTriplesInOntology Then ONLY 
       Ontology triples have been extracted  */

    if( ( res.getTotal()== 0 ) || 
          ( merge && ( res.getTotal() - totalTriplesInOntology ) ==  0 ) ) {
       InOut.removeFiles( res.getGeneratedFiles() )  ;
       
    } else {
         if( display_messg ) {
             
             if( ! res.getGeneratedFiles().isEmpty() ) {
           
                res.getGeneratedFiles().forEach( outF -> {
                    System.out.println("  -> Generated File : " + outF ) ;
                }) ;
            }
         }
    }
        
    return res ;
  }
 
  private static String extractPrefixeclaration ( String stringFile ) {
      
    Pattern p   = Pattern.compile( "^\\[PrefixDeclaration\\].*\\s+.collection \\[\\[" ,
                                   Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE ) ;
    Matcher m   = p.matcher(stringFile)  ;
    m.find() ;
    
   return m.group().trim() + "\n\n" ;
   
  }
    
  private static List<Mapping> extractMappings ( String stringFile ) throws Exception {
      
    Pattern p   = Pattern.compile( "^mappingId.*?\n\n" ,  Pattern.CASE_INSENSITIVE | 
                                                          Pattern.DOTALL           | 
                                                          Pattern.MULTILINE      ) ;
    Matcher m   = p.matcher(stringFile)      ;
    
    List<Mapping> mapings = new ArrayList()  ;
         
    while (m.find()) {
        String[] parts = m.group().trim().split("\n")             ;
        String id      = parts[0].replace("mappingId", "").trim() ;
        String target  = parts[1].replace("target", "").trim()    ;
        String query   = parts[2].replace("source", "").trim()    ;
        
        mapings.add( new Mapping(id, target, query )) ;
    }
  
    return mapings ;
  }
  
  private static String extractUrl ( String fromHeader ) {
    
     Pattern p   = Pattern.compile( "jdbc.url.*=.*$" , Pattern.MULTILINE ) ;
    Matcher m   = p.matcher(fromHeader)                                        ;
    boolean find = m.find()                                                    ;

    if(!find) {
        System.out.println(" -> [ jdbc.url ] Not Found in the Connection File ") ;
        System.out.println( "  " )                                               ;
        System.exit(2)                                                           ;
    }
      
    return  m.group().replaceAll(" +", " ").split("=", 2 )[1].trim() ;
  }

  private static String extractUserName ( String fromHeader ) {
      
    Pattern p   = Pattern.compile( "jdbc.user.*=.*$" , Pattern.MULTILINE ) ;
    Matcher m   = p.matcher(fromHeader)                                        ;
    boolean find = m.find()                                                    ;

    if(!find) {
        System.out.println(" -> [ jdbc.user ] Not Found in the Connection File ") ;
        System.out.println( "  " )                                                ;
        System.exit(2)                                                            ;
    }
      
    return  m.group().replaceAll(" +", " ").split("=", 2 )[1].trim() ;
  }
  
  private static String extractPassword ( String fromHeader ) {
      
    Pattern p   = Pattern.compile( "jdbc.password.*=.*$" , Pattern.MULTILINE ) ;
    Matcher m   = p.matcher(fromHeader)                                        ;
    boolean find = m.find()                                                    ;

    if(!find) {
        System.out.println(" -> [ jdbc.password ] Not Found in the Connection File ") ;
        System.out.println( "  " )                                                    ;
        System.exit(2)                                                                ;
    }
      
    return  m.group().replaceAll(" +", " ").split("=", 2 )[1].trim() ;
    
  }
  
  private static String extractDriver ( String fromHeader ) {
     
    Pattern p   = Pattern.compile( "jdbc.driver.*=.*$" , Pattern.MULTILINE ) ;
    Matcher m   = p.matcher(fromHeader)  ;  
    boolean find = m.find();

    if(!find) {
        System.out.println(" -> [ jdbc.driver ] Not Found in the Connection File ") ;
        System.out.println( "  " )                                                  ;
        System.exit(2)                                                              ;
    }
      
    return  m.group().replaceAll(" +", " ").split("=", 2 )[1].trim() ;
  }

 }
