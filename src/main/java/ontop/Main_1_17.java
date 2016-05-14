
package ontop ;

import java.io.File ;
import java.util.List ;
import java.util.Arrays ;
import java.util.ArrayList ;
import java.net.URLDecoder ;
import java.util.regex.Pattern ;
import it.unibz.krdb.obda.model.OBDAModel ;
import it.unibz.krdb.obda.io.ModelIOManager ;
import java.io.UnsupportedEncodingException ;
import org.semanticweb.owlapi.model.OWLObject ;
import org.semanticweb.owlapi.model.OWLOntology ;
import it.unibz.krdb.obda.model.OBDADataFactory ;
import org.semanticweb.owlapi.apibinding.OWLManager ;
import org.semanticweb.owlapi.model.OWLOntologyManager ;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl ;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWL ;
import it.unibz.krdb.obda.owlrefplatform.core.QuestConstants ;
import it.unibz.krdb.obda.owlrefplatform.core.QuestPreferences ;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWLFactory ;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWLStatement ;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWLResultSet ;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWLConnection ;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWLConfiguration ;

public class Main_1_17 {
 
    private final int flushCount = 10000 ;

    private final OWLOntology ontology   ;
    private final OBDAModel   obdaModel  ;
    
    private final String STR_DTYPE     = "^^xsd:string" ;
    private final String RDF_TYPE_URI  = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>"                                       ;
    private final String URI_VALIDATOR = "^((https?|ftp|file)://|(www\\.))[-a-zA-Z0-9+&@#/%?=~_|!:,.;µs%°]*[-a-zA-Z0-9+&@#/%=~_|]" ;

    private final List<String> XSD     = Arrays.asList( "string", "integer", "decimal","double", "dateTime", "boolean" )           ;
    
    private Main_1_17 (String owlFile, String obdaFile) throws Exception {
        ontology   = loadOWLOntology(owlFile) ;
        obdaModel  = loadOBDA(obdaFile)       ;
    }
    
    public void run( String query, String outputFile , Boolean turtleOut ) throws Exception       {

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
        QuestOWLFactory factory      = new QuestOWLFactory();
        QuestOWLConfiguration config = QuestOWLConfiguration.builder()
                                                            .obdaModel(obdaModel)
                                                            .preferences(preference)
                                                            .build() ;
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
            List<String>  lines      =  new ArrayList<>()   ;
            List<String>  line       =  new ArrayList()     ;
            
            int           loop       =  0                   ;
            int           columnSize =  rs.getColumnCount() ;

            if( turtleOut && columnSize != 3 ) {
                System.out.print  (" Query must have exactly 3 variables ( subject, predicate, object ) " ) ;
                System.out.println(" when Tuttle format is activated (-ttl ) " )                            ;
                System.out.println(" See https://www.w3.org/TR/turtle  " )                                  ;
                System.out.println(" Or try without -ttl parameter " )                                      ; 
                return ;
            }
        
            while (rs.nextRow()) {
                
                line.clear() ;
                
                for (int idx = 1; idx <= columnSize; idx++) {
                                        
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
                     loop++                                  ;
                }
                
                if( loop >= flushCount ) {
                     Writer.writeTextFile(lines, outputFile ) ;                     
                     lines.clear()                            ;
                     loop = 0                                 ;
                }
            }
            
            if( !lines.isEmpty() ) {
                  Writer.writeTextFile(lines, outputFile ) ;
                  lines.clear()                            ;
                  loop = 0                                 ;
            }
        } 
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
                                               .split(Pattern.quote("^^xsd:"))[0] , "UTF-8" )
                                               .replaceAll("\"", "'") + "\"" + xsdType )    ;
                }
                else
                    if( line.get(2).startsWith("<") && line.get(2).endsWith(">") ) {
                        line.set(0, "\"" + URLDecoder.decode(line.get(2)
                                                     .substring(1, line.get(2)
                                                     .lastIndexOf(">")) ,"UTF-8")
                                                     .replaceAll("\"", "'") + 
                                                     "\"" + STR_DTYPE  )    ;
                    }
            }
        }
        
        return line ;
    }
    
    private OBDAModel loadOBDA(String obdaFile) throws Exception {
        OBDADataFactory factory        = OBDADataFactoryImpl.getInstance()  ;
        OBDAModel       localobdaModel = factory.getOBDAModel()             ;
        ModelIOManager  ioManager      = new ModelIOManager(localobdaModel) ;
        ioManager.load(obdaFile)                                            ;
        return localobdaModel                                               ;
    }

    private OWLOntology loadOWLOntology(String owlFile) throws Exception  {
       OWLOntologyManager manager = OWLManager.createOWLOntologyManager() ;
       return manager.loadOntologyFromOntologyDocument(new File(owlFile)) ;
    }

   private boolean isURI( String path ) { 
       if(path.startsWith("<") && path.endsWith(">")) {
          return path.substring(1, path.lastIndexOf(">")).matches(URI_VALIDATOR) ;
       }
       return false ;
   }
   
   private boolean isRDFtype ( String string ) {
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
     
    /**
     * Main client program
     * @param args
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
          
       final String defaultSparqlQuery =  "SELECT DISTINCT ?S ?P ?O { ?S ?P ?O . } " ;

       String owlFile = "", obdaFile = "", outFile = "", q = ""  ;
       
       boolean turtleOut  = false                                ;
       boolean existQuery = false                                ;
        
        for ( int i = 0 ; i < args.length ; i++ ) {
            
            String token = args[i]                         ;
           
            switch ( token ) {
                    case "-owl"  :  owlFile   = args[i+1]  ;
                                    break ;
                    case "-obda" :  obdaFile  = args[i+1]  ;
                                    break ;
                    case "-out"  :  outFile   = args[i+1]  ;
                                    break ;
                    case "-ttl"  :  turtleOut = true       ;
                                    break ;
                    case "-q"    :  q = args[i+1]          ;  
                                    existQuery = true      ;
                                    break ;
            }
        }
       
        System.out.println(" owl        =  " + owlFile )   ;
        System.out.println(" obda       =  " + obdaFile )  ;
        System.out.println(" out        =  " + outFile )   ;
        System.out.println(" q          =  " + q )         ;
        System.out.println(" outFormat  =  " + turtleOut ) ;
        
        if( owlFile.isEmpty() || obdaFile.isEmpty() || outFile.isEmpty() ) {
           System.out.println(" Missing parameters !! ")         ;
           return                                                ;
        }
        
        if( !existQuery ) {
            q         = defaultSparqlQuery                       ;
            turtleOut = true                                     ;
        }
                
        Writer.checkFile( outFile )                              ;
        
        Main_1_17 ontop  = new Main_1_17 ( owlFile, obdaFile )   ;
        ontop.run( q, outFile , turtleOut )                      ;
        
    }
}
