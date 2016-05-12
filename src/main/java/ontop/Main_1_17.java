package ontop;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.net.URLDecoder;
import java.util.regex.Pattern;
import it.unibz.krdb.obda.model.OBDAModel;
import it.unibz.krdb.obda.io.ModelIOManager;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import it.unibz.krdb.obda.model.OBDADataFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWL;
import it.unibz.krdb.obda.owlrefplatform.core.QuestConstants;
import it.unibz.krdb.obda.owlrefplatform.core.QuestPreferences;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWLFactory;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWLStatement;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWLResultSet;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWLConnection;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWLConfiguration;
import java.util.Arrays;

public class Main_1_17 {
 
    private final int flushCount = 10000 ;

    private final OWLOntology ontology   ;
    private final OBDAModel   obdaModel  ;
    
    private final String DATATYPE      = "^^xsd:string" ;
    private final String RDF_TYPE_URI  = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>" ;
    private final String URI_VALIDATOR = "^((https?|ftp|file)://|(www\\.))[-a-zA-Z0-9+&@#/%?=~_|!:,.;µs%°]*[-a-zA-Z0-9+&@#/%=~_|]" ;

    private final List<String> XSD     = Arrays.asList( "string", "integer", "decimal","double", "dateTime", "boolean" ) ;
    
    private Main_1_17 (String owlFile, String obdaFile) throws Exception {
        ontology   = loadOWLOntology(owlFile) ;
        obdaModel  = loadOBDA(obdaFile) ;
    }
    
    public void run( String query, String outputFile ) throws Exception {

        QuestPreferences preference = new QuestPreferences();
        preference.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL) ;
        preference.setCurrentValueOf(QuestPreferences.REWRITE, "true") ;
        preference.setCurrentValueOf(QuestPreferences.OBTAIN_FROM_MAPPINGS, QuestConstants.TRUE)  ;
        preference.setCurrentValueOf(QuestPreferences.OBTAIN_FROM_ONTOLOGY, QuestConstants.TRUE)  ;
        preference.setCurrentValueOf(QuestPreferences.REFORMULATION_TECHNIQUE, QuestConstants.TW) ;
	preference.setCurrentValueOf(QuestPreferences.OPTIMIZE_EQUIVALENCES, "true") ;
	preference.setCurrentValueOf(QuestPreferences.DBTYPE, QuestConstants.SEMANTIC_INDEX) ; 

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
              QuestOWL reasoner       = factory.createReasoner(ontology, config);
              QuestOWLConnection conn = reasoner.getConnection() ;
              QuestOWLStatement st    = conn.createStatement()   ;
              QuestOWLResultSet rs    = st.executeTuple(query)   ;
        )
        {
            List<String> lines = new ArrayList<>() ;
            String result = "" ;
            int  loop     = 0  ;
            
            int columnSize = rs.getColumnCount() ;

            String s = "", p = "", o = "" ;
            
            while (rs.nextRow()) {
                
                for (int idx = 1; idx <= columnSize; idx++) {
                                        
                    OWLObject binding = rs.getOWLObject(idx) ;
                    
                    if( idx == 1 ) { 
                        s = binding.toString() ; continue ; 
                    }
                    if( idx == 2 ) { 
                        p = binding.toString() ; continue ; 
                    }
                    if( idx == 3 ) { 
                        o = binding.toString() ; continue ;
                    }
                }

                if( ! isURI(s) ) continue ; 
                                  
                if(isRDFtype(s))  s = RDF_TYPE_URI ;
                else { 
                       s = URLEncoder.encode(s)    ;
                } 
                
                if(isRDFtype(p))  p = RDF_TYPE_URI ;
                else {
                    if(isURI(p))  p = URLEncoder.encode(p) ;
                }
                 
                if(isRDFtype(o))  o = RDF_TYPE_URI ;   
                else {
                    if(isURI(o) )    
                        o = URLEncoder.encode(o)   ;
                    else 
                    {
                        String xsdType = getXSDType(o);
                        if(xsdType != null ) {
                            o = "\"" + URLDecoder.decode( o.substring( 1, o.lastIndexOf(">"))
                                           .split(Pattern.quote("^^xsd:"))[0] , 
                                           "UTF-8" ).replaceAll("\"", "'") + "\"" + xsdType ;
                        }
                        else
                        if( o.startsWith("<") && o.endsWith(">")) {
                            o = "\"" + URLDecoder.decode(o.substring(1, o.lastIndexOf(">"))
                                       ,"UTF-8").replaceAll("\"", "'") + "\"" + DATATYPE  ;
                        }
                    }
                }

                lines.add( s + " " + p +" " + o + " ." ) ;

                loop++ ;
                  
                if( !result.isEmpty())  {
                     lines.add(result) ;
                     result =  ""      ;
                     loop++            ;
                 }
                  
                if(loop >= flushCount ) {
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
       return XSD.contains(xsd);
   }
     
    /**
     * Main client program
     * @param args
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
          
       final String defaultSparqlQuery =  "SELECT DISTINCT ?S ?P ?O { ?S ?P ?O . } " ;

       String owlFile = "", obdaFile = "", outFile = "", q = "" ;
       
       int nbParams = 0 ;
       
        if( args.length < 6 ) {
             System.out.println(" missing parameters !! ") ;
             return ;
        }
            
        boolean existQuery = false ;
        for ( int i = 0 ; i < args.length ; i++ ) {
            
            String token = args[i];
           
            switch(token) {
                case "-owl"  :  owlFile = args[i+1] ; nbParams += 2 ;
                                break ;
                case "-obda" :  obdaFile = args[i+1]; nbParams += 2 ;
                                break ;
                case "-out"  :  outFile = args[i+1] ; nbParams += 2 ;
                                break ;
                case "-q"    :  q = args[i+1]   ; existQuery = true ;
                                break ;
            }
        }
       
        System.out.println(" owl  =  " + owlFile)  ;
        System.out.println(" obda =  " + obdaFile) ;
        System.out.println(" out  =  " + outFile)  ;
        System.out.println(" q    =  " + q)        ;
        
        if( nbParams < 6 ) {
           System.out.println(" missing parameters !! ") ;
           return ;
        }
        
        Main_1_17 ontop   =  new Main_1_17 ( owlFile, obdaFile ) ;
       
        if(!existQuery) q = defaultSparqlQuery                   ;
                
        Writer.checkFile(outFile)                                ;
        
        ontop.run(q, outFile )                                   ;
        
    }
}
