
package ontop ;

import java.util.List ;
import java.util.ArrayList ;
import ontop.entity.Mapping ;
import java.util.regex.Matcher ;
import java.util.regex.Pattern ;
import java.util.stream.Collectors ;

/**
 *
 * @author ryahiaoui
 */
public class Manager {
    
  private static int totalTriplesInOntology       = 0  ;
    
  public static void process ( String  owlFile         ,
                               String  obdaFile        ,
                               String  sparqlQuery     ,
                               String  outFile         ,
                               boolean turtleOutFormat ,
                               boolean batch           ,
                               int     pageSize        ,
                               int     fragment        ,
                               boolean merge           ,
                               int     flushCount )    throws Exception  {
      
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
       Processor ontop  = new Processor ( owlFile, obdaFile ) ;
       ontop.run( sparqlQuery     , 
                  outFile         , 
                  turtleOutFormat , 
                  fragment        ,
                  flushCount      )                           ;
       return                                                 ;
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
    
    int currentPage  = 0  ; 
    int index        = 0  ; 
    
    if( ! mappings.isEmpty() ) {
       
        Mapping emptyMapping  = new Mapping( "" , "" , "") ;
      
        String ontoObdaFileName =  subFolder + "emptyMapping.obda"   ;
      
        /* Obda File */
        InOut.writeTextFile( emptyMapping.emptyMapping(), ontoObdaFileName  ) ;        
        
        totalTriplesInOntology = processMapping ( emptyMapping      ,
                                                  ontoObdaFileName  ,
                                                  owlFile           ,
                                                  outFile           ,
                                                  index ++          ,
                                                  sparqlQuery       ,
                                                  turtleOutFormat   ,
                                                  fragment          ,
                                                  flushCount      ) ;
    }
 
    if( ! merge )  {
        owlFile =  subFolder + "emptyOwl.owl"   ;
        InOut.writeTextFile( "" , owlFile  )    ;
        totalTriplesInOntology  =  0            ;
    }
    
    for( Mapping mapping : mappings )  {
        
        if( pageSize >= 0 ) {
            
            /* Apply Params for current Sub Mapping */ 
            List<String> extractHeader = sqlManager.extractHeader(mapping.getQuery() ) ;
            mapping.applyParams(extractHeader, pageSize , sqlManager.getDbProvider() ) ;

            currentPage = 0                      ;
            mapping.applyOffset( currentPage ++) ;
            InOut.writeTextFile( mapping.toString(), subOBDAFilePath  ) ;

            while( processMapping ( mapping                ,
                                    subOBDAFilePath        ,
                                    owlFile                ,
                                    outFile                ,
                                    index   ++             ,
                                    sparqlQuery            ,
                                    turtleOutFormat        ,
                                    fragment               ,
                                    flushCount             )  
                                    - totalTriplesInOntology > 0 )  {

                /* Obda File */
                mapping.applyOffset(  ( ++currentPage -1 ) * pageSize   )   ;
                InOut.writeTextFile( mapping.toString(), subOBDAFilePath  ) ;
           }
        }
        else {
            
             processMapping ( mapping            ,
                              subOBDAFilePath    ,
                              owlFile            ,
                              outFile            ,
                              index   ++         ,
                              sparqlQuery        ,
                              turtleOutFormat    ,
                              fragment           ,
                              flushCount       ) ; 
        }
    }
            
  }
  
  private static int processMapping ( Mapping mapping         ,
                                      String  subFileObda     , 
                                      String  owlFile         ,
                                      String  outFiles        ,
                                      int     index           ,
                                      String  sparqlQuery     ,
                                      boolean turtleOutFormat ,
                                      int     fragment        ,
                                      int     flushCount      ) throws Exception {
 
        /* out path result */
        String out =  outFiles + "_" + index ++     +
                      "_" + Mapping.FILE_NAME       +
                      "_" + mapping.getCodeFromId() +
                      "_" + Mapping.PAGE_SIZE       +
                      "_" + mapping.getOFFSET()     + 
                      Mapping.EXTENSION             ;
        
        /* Process extraction */
        
        Processor ontop =  new Processor ( owlFile, subFileObda ) ;
        
        int       res   =  ontop.run ( sparqlQuery     , 
                                       out             , 
                                       turtleOutFormat , 
                                       fragment        ,
                                       flushCount      )  ;
        
        if( ( res - totalTriplesInOntology ) ==  0 ) {
          InOut.removeFile( out )  ;
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
        
        mapings.add( new Mapping(id, target, query )) ;
    }
  
    return mapings ;
  }
  
  
  private static String extractHeader ( String stringFile ) {
      
    Pattern p   = Pattern.compile( "^\\[PrefixDeclaration\\].*\\s+.collection \\[\\[" ,
                                   Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE ) ;
    Matcher m   = p.matcher(stringFile)  ;
    m.find() ;
    
   return m.group().trim() + "\n\n" ;
   
  }
  
  private static String extractUrl ( String fromHeader ) {
    Pattern p   = Pattern.compile( "connectionUrl.*.?\n" , Pattern.MULTILINE ) ;
    Matcher m   = p.matcher(fromHeader)  ;
    m.find() ;
    return m.group().split("connectionUrl")[1].trim() ;
  }

  private static String extractUserName ( String fromHeader ) {
    Pattern p   = Pattern.compile( "username.*.?\n" , Pattern.MULTILINE ) ;
    Matcher m   = p.matcher(fromHeader)  ;
    m.find() ;
    return m.group().split("username")[1].trim() ;
  }
  
  private static String extractPassword ( String fromHeader ) {
    Pattern p   = Pattern.compile( "password.*.?\n" , Pattern.MULTILINE ) ;
    Matcher m   = p.matcher(fromHeader)  ;
    m.find() ;
   return m.group().split("password")[1].trim() ;
  }
  
  private static String extractDriver ( String fromHeader ) {
    Pattern p   = Pattern.compile( "driverClass.*.?\n" , Pattern.MULTILINE ) ;
    Matcher m   = p.matcher(fromHeader)  ;
    m.find() ;
    return m.group().split("driverClass")[1].trim() ;
  }

}
