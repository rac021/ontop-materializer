
package ontop ;

import java.io.File                                                   ;
import java.util.List                                                 ;
import java.util.Arrays                                               ;
import java.util.ArrayList                                            ;
import java.net.URLDecoder                                            ;
import java.util.regex.Pattern                                        ;
import it.unibz.inf.ontop.model.OBDAModel                             ;
import it.unibz.inf.ontop.io.ModelIOManager                           ;
import java.io.UnsupportedEncodingException                           ;
import org.semanticweb.owlapi.model.OWLObject                         ;
import it.unibz.inf.ontop.model.OBDADataFactory                       ; 
import org.semanticweb.owlapi.model.OWLOntology                       ;
import org.semanticweb.owlapi.apibinding.OWLManager                   ;
import org.semanticweb.owlapi.model.OWLOntologyManager                ;
import it.unibz.inf.ontop.model.impl.OBDADataFactoryImpl              ;
import it.unibz.inf.ontop.owlrefplatform.owlapi.QuestOWL              ;
import it.unibz.inf.ontop.owlrefplatform.core.QuestConstants          ;
import it.unibz.inf.ontop.owlrefplatform.core.QuestPreferences        ;
import it.unibz.inf.ontop.owlrefplatform.owlapi.QuestOWLFactory       ;
import it.unibz.inf.ontop.owlrefplatform.owlapi.QuestOWLResultSet     ;
import it.unibz.inf.ontop.owlrefplatform.owlapi.QuestOWLStatement     ;
import it.unibz.inf.ontop.owlrefplatform.owlapi.QuestOWLConnection    ;
import it.unibz.inf.ontop.owlrefplatform.owlapi.QuestOWLConfiguration ;

public class Processor {
 
    private final   OWLOntology ontology   ;
    private final   OBDAModel   obdaModel  ;
    
    private final String STR_DTYPE     = "^^xsd:string"                                                                            ;
    private final String RDF_TYPE_URI  = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>"                                       ;
    private final String URI_VALIDATOR = "^((https?|ftp|file)://|(www\\.))[-a-zA-Z0-9+&@#/%?=~_|!:,.;µs%°]*[-a-zA-Z0-9+&@#/%=~_|]" ;

    private final List<String> XSD     = Arrays.asList ( "string" , "integer"  , "decimal" ,
                                                         "double" , "dateTime" , "date"    , 
                                                         "time"   , "duration" , "boolean" ) ;
    
    Processor ( String owlFile, String obdaFile ) throws Exception {
        ontology   = loadOWLOntology(owlFile) ;
        obdaModel  = loadOBDA(obdaFile)       ;
    }
    
    public int run( String query        , 
                    String outputFile   , 
                    Boolean turtleOut   , 
                    int fragment        ,
                    int flushCount      ) throws Exception                                        {

        QuestPreferences preference = new QuestPreferences() ;
        preference.setCurrentValueOf(QuestPreferences.OBTAIN_FROM_MAPPINGS, QuestConstants.TRUE)  ;
        preference.setCurrentValueOf(QuestPreferences.OBTAIN_FROM_ONTOLOGY, QuestConstants.TRUE)  ;
        preference.setCurrentValueOf(QuestPreferences.REFORMULATION_TECHNIQUE, QuestConstants.TW) ;
        preference.setCurrentValueOf(QuestPreferences.DBTYPE, QuestConstants.SEMANTIC_INDEX)      ; 
        preference.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL)          ;
        preference.setCurrentValueOf(QuestPreferences.OPTIMIZE_EQUIVALENCES, "true")              ;
        preference.setCurrentValueOf(QuestPreferences.REWRITE, "true")                            ;

        /*
         * Create the instance of Quest OWL reasoner.
         */
        QuestOWLFactory factory      = new QuestOWLFactory() ;
        QuestOWLConfiguration config = QuestOWLConfiguration.builder()
                                                            .obdaModel(obdaModel)
                                                            .preferences(preference)
                                                            .build()    ;
        
        int     TOTAL_EXTRACTION     =  0                               ;
        int     fragCounter          =  0                               ;
        
        String  currentFile          =  outputFile                      ; 
        
        String folder                = InOut.getFolder ( outputFile )   ;
        String fileName              = InOut.getfileName ( outputFile ) ;
        String extension             = InOut.getFileExtension(fileName) ; 
     
         /*
         * Prepare the data connection for querying.
         */
        try (
              QuestOWL reasoner       = factory.createReasoner(ontology, config) ;
              QuestOWLConnection conn = reasoner.getConnection() ;
              QuestOWLStatement  st   = conn.createStatement()   ;
              QuestOWLResultSet  rs   = st.executeTuple(query)   ;
        )
        {
            List<String>  lines      =  new ArrayList<>()        ;
            List<String>  line       =  new ArrayList()          ;

            int           loopForFlush  =  0                     ;
            int           columnSize    =  rs.getColumnCount()   ;
             
            if( turtleOut && columnSize != 3 ) {
                System.out.print  (" Query must have exactly 3 variables ( subject, predicate, object ) " ) ;
                System.out.println(" when Tuttle format is activated (-ttl ) " )                            ;
                System.out.println(" See https://www.w3.org/TR/turtle  " )                                  ;
                System.out.println(" Or try without -ttl parameter " )                                      ; 
                System.exit(1) ;
            }
        
            while (rs.nextRow()) {
                
                line.clear() ;
                
                for (int idx = 1; idx <= columnSize; idx++)  {
                                        
                    OWLObject binding = rs.getOWLObject(idx) ;
                    
                    line.add(binding.toString())             ;
                }
                
                if( turtleOut ) {

                    line = turtleAdapt (line)                ;
                }
               
                if( !line.isEmpty() ) {
                    
                     lines.add( line
                          .stream()
                          .reduce( ( t, u )-> t + " " + u )
                          .get() + " ." )                    ;
                     
                     loopForFlush     ++                     ;
                     TOTAL_EXTRACTION ++                     ;
                     
                }
                
                if( fragment != 0 && TOTAL_EXTRACTION % fragment == 0  ) {
                            
                    if( ! lines.isEmpty() )  {
                        InOut.createFileIfNotExists(currentFile)  ;
                        InOut.writeTextFile(lines, currentFile )  ;
                        lines.clear()                             ;
                        loopForFlush = 0                          ;
                        currentFile  =  getCurrentFile( outputFile      , 
                                                        extension       , 
                                                        ++fragCounter ) ;
                    }
                }
                 
                if ( loopForFlush >= flushCount )                {
                    
                    if( ! lines.isEmpty() )     {
                        /* Append to an existing file */
                        InOut.createFileIfNotExists(currentFile) ;
                        InOut.writeTextFile(lines, currentFile ) ;
                        lines.clear()                            ;
                        loopForFlush  = 0                        ;
                    }
                }
            }
            
            if( !lines.isEmpty() ) {
                
                if ( loopForFlush >= flushCount )                       {
                       currentFile  =  getCurrentFile ( outputFile      , 
                                                        extension       ,
                                                        ++fragCounter ) ;
                }
                
                InOut.createFileIfNotExists(currentFile)  ;
                InOut.writeTextFile(lines, currentFile )  ;
                lines.clear()                             ;
                loopForFlush  = 0                         ;
                
            }
        } 
        
        return TOTAL_EXTRACTION  ;
    }

    private List turtleAdapt ( List<String> line ) throws UnsupportedEncodingException {
        
        if (! isURI(line.get(0))) {
              line.clear() ;
              return line  ;
        }
        
        if( isRDFtype(line.get(0)) )  
            line.set(0, RDF_TYPE_URI )                         ;
        else {
            line.set(0, URLEncoder.encode(line.get(0)) )       ;
        }
        
        if( isRDFtype( line.get(1)) )  
            line.set(1, RDF_TYPE_URI )                         ;
        else {
            if( isURI(line.get(1)) )  
                line.set(1, URLEncoder.encode(line.get(1)))    ;
        }
        
        if( isRDFtype( line.get(2)) )  
            line.set(2, RDF_TYPE_URI )                         ;
        else {
            if( isURI(line.get(2)) )
                line.set(2, URLEncoder.encode(line.get(2)) )   ;
            else
            {
               String xsdType = getXSDType(line.get(2))        ;
               
               if(xsdType != null ) {
                  line.set( 2,"\"" + URLDecoder.decode( line.get(2)
                                               .substring( 1, line.get(2)
                                               .lastIndexOf(">") )
                                               .split( Pattern
                                               .quote("^^xsd:"))[0] , "UTF-8" )
                                               .replaceAll("\"", "'") 
                                               .replaceAll("\n", " ") 
                                                + "\"" + xsdType )  ;
                }
               
                else
                    if( line.get(2).startsWith("<") && line.get(2).endsWith(">") ) {
                        line.set(2, "\"" + URLDecoder.decode(line.get(2)
                                                     .substring(1, line.get(2)
                                                     .lastIndexOf(">")) ,"UTF-8")
                                                     .replaceAll("\"", "'")
                                                     .replaceAll("\n", " ") + 
                                                     "\"" + STR_DTYPE  )    ;
                    }
            }
        }
        
        return line ;
    }
    
    private OBDAModel loadOBDA(String obdaFile) throws Exception           {
       if(obdaFile == null ) return null ;
       OBDADataFactory factory        = OBDADataFactoryImpl.getInstance()  ;
       OBDAModel       localobdaModel = factory.getOBDAModel()             ;
       ModelIOManager  ioManager      = new ModelIOManager(localobdaModel) ;
       ioManager.load(obdaFile)                                            ;
       return localobdaModel                                               ;
    }

    private OWLOntology loadOWLOntology(String owlFile) throws Exception  {
       if(owlFile == null ) return null ;
       OWLOntologyManager manager = OWLManager.createOWLOntologyManager() ;
       return manager.loadOntologyFromOntologyDocument(new File(owlFile)) ;
    }

   private boolean isURI( String path ) { 
       if(path.startsWith("<") && path.endsWith(">")) {
          return path.substring(1, path.lastIndexOf(">")).matches(URI_VALIDATOR) ;
       }
       return false ;
   }
   
   private boolean isRDFtype ( String string )         {
       return  string.toLowerCase().equals("rdf:type") ;
   }
   
   private String getXSDType ( String value ) {
      if(value.startsWith("<")    && 
         value.  endsWith(">")    && 
         value.contains("^^xsd:") &&
         valideXSD(value.substring(1, value.lastIndexOf(">"))
                                .split(Pattern.quote("^^xsd:"))[1]) ) {
         return "^^xsd:" + value.substring(1, value.lastIndexOf(">"))
                                .split(Pattern.quote("^^xsd:"))[1]  ;
      }
      return null ;
   }
   
   private boolean valideXSD( String xsd ) {
       return XSD.contains(xsd) ;
   }

   
   private static String getCurrentFile(  String outFile , String extension , int fragment  )   {
     
      if ( fragment <= 0 ) {
           return outFile  ; 
      }
    
     return outFile.replace(extension, "") + "_" + fragment + extension ;             
   }    

}
