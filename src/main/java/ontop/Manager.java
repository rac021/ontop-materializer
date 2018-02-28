
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
    
  private static int totalTriplesInOntology     = 0    ;
  
  public static boolean  output_ontology        = true ;
   
  public static void process ( String  owlFile             ,
                               String  obdaFile            ,
                               String  sparqlQuery         ,
                               String  outFile             ,
                               boolean turtleOutFormat     ,
                               boolean batch               ,
                               int     pageSize            ,
                               int     fragment            ,
                               boolean merge               ,
                               int     flushCount          ,
                               List<String> mustNotBeEmpty ,
                               boolean debug               ) throws Exception  {
      
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
    
    if( ! batch ) {
        
        /* BONUS */
        
        if( output_ontology ) {
       
            /* Transform File to String */
            String stringFile = InOut.readTextFile(obdaFile )
                                     .stream()
                                     .collect (Collectors.joining ("\n")) ;

            /* extract Header from obda File */
            String fileName   = InOut.getfileName ( obdaFile )            ;

            Mapping.header    =  extractHeader(stringFile)                ;
            Mapping.PAGE_SIZE =  pageSize                                 ;
            Mapping.FILE_NAME =  InOut.getFileWithoutExtension(fileName ) ;

            Mapping.PAGE_SIZE =  pageSize                                 ;
            Mapping.FILE_NAME =  InOut.getFileWithoutExtension(fileName ) ;

            /*   Create tmp folder for sub-mappings */
            String subFolder          = InOut.extractFolder( obdaFile ) + "subMappings/" ;
            String subOBDAFilePath    = subFolder + "mapping.obda"                       ;
            InOut.deleteAndCreateFile   ( subOBDAFilePath    )                           ;

            if( outFile.endsWith("/")) {
                outFile = outFile.substring (0 , outFile.length() - 1 ) ;
            }
            
            String outOntology        = InOut.getFolder ( outFile ) + "/Ontology.ttl"    ;

            Mapping emptyMapping      = new Mapping( "0" , "" , "")                      ;

            String  ontoObdaFileName  = subFolder + "emptyMapping.obda"                  ;

            /* Obda File */
            InOut.writeTextFile( emptyMapping.emptyMapping(), ontoObdaFileName  )        ;

            System.out.println(" Process Ontology ..." )                   ;
       
            totalTriplesInOntology = processMappingOnto( ontoObdaFileName  ,
                                                         owlFile           ,
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

        ResultInfo res = new Processor ( owlFile , 
                                         obdaFile  
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
    String stringFile = InOut.readTextFile(obdaFile )
                             .stream()
                             .collect (Collectors.joining ("\n")) ;
    
    /* extract Header from obda File */
    String fileName  = InOut.getfileName ( obdaFile )             ;

    Mapping.header    =  extractHeader(stringFile)                ;
    Mapping.PAGE_SIZE =  pageSize                                 ;
    Mapping.FILE_NAME =  InOut.getFileWithoutExtension(fileName ) ;
                 
    List<Mapping> mappings = extractMappings(stringFile) ;
   
    SqlManager sqlManager = new SqlManager( extractDriver( Mapping.header )     ,
                                            extractUrl( Mapping.header )        ,
                                            extractUserName( Mapping.header )   ,
                                            extractPassword( Mapping.header ) ) ;
    
    /*   Create tmp folder for sub-mappings */
    String subFolder          = InOut.extractFolder( obdaFile ) + "subMappings/" ;
    String subOBDAFilePath    =  subFolder + "mapping.obda"                      ;
    InOut.deleteAndCreateFile ( subOBDAFilePath    )                             ;
    
    int     currentPage = 0     ; 
    int     index       = 0     ; 
    boolean exists_ttl  = false ; 
    
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

    if( mustNotBeEmpty != null && ! mustNotBeEmpty.isEmpty() ) {
       // Reorder Mappings by putting thows are in the mustNotBeEmpty in first
          reorderList( mappings , mustNotBeEmpty ) ;
    }   
    
    for( Mapping mapping : mappings )  {
        
        if( pageSize >= 0 ) {
            
          System.out.println(" Process Node : " + mapping.getCodeFromId() + " ..." ) ;
          
          /* Apply Params for current Sub Mapping */ 
          List<String> columns = sqlManager.extractColumns(mapping.getQuery() ) ;
          mapping.applyParams(columns, pageSize , sqlManager.getDbProvider() )  ;

          currentPage = 0                      ;
          mapping.applyOffset( currentPage ++) ;
          InOut.writeTextFile( mapping.toString(), subOBDAFilePath  ) ;
          exists_ttl = false ;
          
          while( processMapping ( mapping                ,
                                  subOBDAFilePath        ,
                                  owlFile                ,
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
              if( ! exists_ttl )   exists_ttl = true                      ;
           }
          
           if( ! exists_ttl && 
                 existsInList ( mapping.getId() , mustNotBeEmpty ) )     {
                 InOut.removeDirectory( outFile ) ;
                 break ;
           }
           
           System.out.println("  ")                                       ;
           
        }
        else {
            
             System.out.println(" Process Node : " + mapping.getCodeFromId() + " ..." ) ;
             
             int total_result =  processMapping ( mapping            ,
                                                  subOBDAFilePath    ,
                                                  owlFile            ,
                                                  outFile            ,
                                                  index   ++         ,
                                                  sparqlQuery        ,
                                                  turtleOutFormat    ,
                                                  fragment           ,
                                                  flushCount         ,
                                                  debug              ,
                                                  merge              ,
                                                  true             ) . getTotal() 
                                                  - totalTriplesInOntology      ;

               
             if( ( total_result == 0 ) &&  
                 existsInList ( mapping.getId() , mustNotBeEmpty ) )     {
                 InOut.removeDirectory( outFile ) ;
                 break ;
             }
             
             System.out.println("  ")            ;
            
        }
    }
            
  }
  
   private static ResultInfo processMappingOnto ( String  subFileObda     , 
                                                  String  owlFile         ,
                                                  String  outFiles        ,
                                                  String  sparqlQuery     ,
                                                  boolean turtleOutFormat ,
                                                  int     fragment        ,
                                                  int     flushCount      ,
                                                  boolean debug           ,
                                                  boolean output_ontology ) throws Exception {
 
    /* Process extraction */
     
    Processor ontop =  new Processor ( owlFile, subFileObda ) ;
        
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
   
    Processor ontop =  new Processor ( owlFile, subFileObda ) ;
        
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
        
        mapings.add( new Mapping(id, target, query ))             ;
    }
  
    return mapings ;
  }
  
  
  private static String extractHeader ( String stringFile ) {
      
    Pattern p   = Pattern.compile( "^\\[PrefixDeclaration\\].*\\s+.collection \\[\\[" ,
                                   Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE ) ;
    Matcher m   = p.matcher(stringFile)  ;
    m.find()                             ;
    
   return m.group().trim() + "\n\n"      ;
   
  }
  
  private static String extractUrl ( String fromHeader ) {
    Pattern p   = Pattern.compile( "connectionUrl.*.?\n" , Pattern.MULTILINE ) ;
    Matcher m   = p.matcher(fromHeader)               ;
    m.find()                                          ;
    return m.group().split("connectionUrl")[1].trim() ;
  }

  private static String extractUserName ( String fromHeader ) {
    Pattern p   = Pattern.compile( "username.*.?\n" , Pattern.MULTILINE ) ;
    Matcher m   = p.matcher(fromHeader)          ;
    m.find()                                     ;
    return m.group().split("username")[1].trim() ;
  }
  
  private static String extractPassword ( String fromHeader ) {
    Pattern p   = Pattern.compile( "password.*.?\n" , Pattern.MULTILINE ) ;
    Matcher m   = p.matcher(fromHeader)         ;
    m.find()                                    ;
   return m.group().split("password")[1].trim() ;
  }
  
  private static String extractDriver ( String fromHeader ) {
    Pattern p   = Pattern.compile( "driverClass.*.?\n" , Pattern.MULTILINE ) ;
    Matcher m   = p.matcher(fromHeader)             ;
    m.find()                                        ;
    return m.group().split("driverClass")[1].trim() ;
  }

  private static boolean existsInList(String id, List<String> mustNotBeEmpty ) {
    return mustNotBeEmpty.stream().anyMatch( i -> i.contains(id)) ;
  }

    private static List<Mapping> reorderList ( List<Mapping> mappings, 
                                               List<String> mustNotBeEmpty ) {
        int i = 0 ; 
        while( i++ < mappings.size() - 1 ) {
             if( mustNotBeEmpty.contains( mappings.get(i).getId())) {
                mappings.add(0, mappings.remove(i));
             }
        }
        
        return mappings ;
    }

}
