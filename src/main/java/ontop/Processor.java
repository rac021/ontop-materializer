
 package ontop ;

 import java.util.List ;
 import java.util.Arrays ;
 import java.util.ArrayList ;
 import java.net.URLDecoder ;
 import java.io.PrintStream ;
 import java.util.LinkedHashSet ;
 import java.util.regex.Pattern ;
 import java.util.logging.Level ;
 import ontop.entity.ResultInfo ;
 import java.util.logging.Logger ;
 import java.io.ByteArrayOutputStream ;
 import java.io.UnsupportedEncodingException ;
 import org.semanticweb.owlapi.model.OWLObject ;
 import it.unibz.inf.ontop.owlapi.OntopOWLFactory ;
 import org.semanticweb.owlapi.model.OWLException ;
 import org.semanticweb.owlapi.io.ToStringRenderer ;
 import it.unibz.inf.ontop.owlapi.OntopOWLReasoner ;
 import it.unibz.inf.ontop.owlapi.resultset.OWLBinding ;
 import it.unibz.inf.ontop.owlapi.resultset.OWLBindingSet ;
 import it.unibz.inf.ontop.owlapi.resultset.TupleOWLResultSet ;
 import it.unibz.inf.ontop.owlapi.connection.OntopOWLStatement ;
 import it.unibz.inf.ontop.owlapi.connection.OntopOWLConnection ;
 import it.unibz.inf.ontop.injection.OntopSQLOWLAPIConfiguration ;
 import it.unibz.inf.ontop.answering.reformulation.impl.SQLExecutableQuery ;

 /**
 *
 * @author ryahiaoui
 */
 public class Processor {
 
    private final String owlFile        ;
    private final String obdaFile       ;
    private final String connectionFile ;
    
    private final String STR_DTYPE     = "^^xsd:string"                                                                            ;
    private final String RDF_TYPE_URI  = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>"                                       ;
    private final String URI_VALIDATOR = "^((https?|ftp|file)://|(www\\.))[-a-zA-Z0-9+&@#/%?=~_|!:,.;µs%°]*[-a-zA-Z0-9+&@#/%=~_|]" ;

    private final List<String> XSD     = Arrays.asList ( "string" , "integer"  , "decimal" ,
                                                         "double" , "dateTime" , "date"    , 
                                                         "time"   , "duration" , "boolean" ) ;
    
    private final static ByteArrayOutputStream OUTPUT_STREAM = new ByteArrayOutputStream()   ;
    
    public static boolean HIDE_SYSTEM_OUT = false ;
    
    Processor ( String owlFile        , 
                String obdaFile       , 
                String connectionFile ) throws Exception {

        this.owlFile        = owlFile        ;
        this.obdaFile       = obdaFile       ;
        this.connectionFile = connectionFile ;

    }
    
    public ResultInfo run( String  query       , 
                           String  outputFile  , 
                           Boolean turtleOut   , 
                           int     fragment    ,
                           int     flushCount  ,     
                           boolean debug       ) throws Exception       {

        LinkedHashSet <String> 
                resultGeneratedFiles =  new LinkedHashSet <>()          ;
        
        int     TOTAL_EXTRACTION     =  0                               ;
        int     fragCounter          =  0                               ;
        
        String  currentFile          =  outputFile                      ; 
        
        String  folder               = InOut.getFolder ( outputFile )   ;
        String  fileName             = InOut.getfileName ( outputFile ) ;
        String  extension            = InOut.getFileExtension(fileName) ; 
           
        OntopOWLFactory factory      = OntopOWLFactory.defaultFactory() ;

        OntopSQLOWLAPIConfiguration config = 
                OntopSQLOWLAPIConfiguration.defaultBuilder()
                                           .nativeOntopMappingFile(obdaFile)
                                           .ontologyFile(owlFile)
                                           .propertyFile(connectionFile)
                                           .enableTestMode()
                                           .build() ;

        PrintStream p = System.out ;

        if( HIDE_SYSTEM_OUT ) {
           System.setOut( new PrintStream ( OUTPUT_STREAM ) )       ;
        }
        
        OntopOWLReasoner  reasoner = factory.createReasoner(config) ;

        if( HIDE_SYSTEM_OUT )    {
           System.setOut( p )    ;
           OUTPUT_STREAM.reset() ;
        }
        
        try ( OntopOWLConnection conn = reasoner.getConnection()    ;
              OntopOWLStatement  st   = conn.createStatement()      ;
              TupleOWLResultSet  rs   = st.executeSelectQuery(query )
        ) {
            
            List<String>  lines         =  new ArrayList<>()     ;
            List<String>  line          =  new ArrayList()       ;

            int           loopForFlush  =  0                     ;
            int           columnSize    =  rs.getColumnCount()   ;
             
            if( turtleOut && columnSize != 3 ) {
                System.out.print  (" Query must have exactly 3 variables ( subject, predicate, object ) " ) ;
                System.out.println(" when Turtle format is activated (-ttl ) " )                            ;
                System.out.println(" See https://www.w3.org/TR/turtle  " )                                  ;
                System.out.println(" Or try without -ttl parameter " )                                      ; 
                System.exit(1) ;
            }

            while (rs.hasNext()) {
                
                final OWLBindingSet bindingSet = rs.next() ;
               
                line.clear() ;
                
                 for (OWLBinding binding : bindingSet)    {
                     OWLObject value = binding.getValue() ;
                     line.add(ToStringRenderer.getInstance().getRendering(value)) ;
                }
               
                if( turtleOut ) {

                    line = turtleAdapt (line) ;
                }
               
                if( !line.isEmpty() ) {
                    
                     lines.add( line
                          .stream()
                          .reduce( ( t, u )-> t + " " + u )
                          .get() + " ." )                 ;
                     
                     loopForFlush     ++                  ;
                     TOTAL_EXTRACTION ++                  ;
                     
                }
                
                if( fragment != 0 && TOTAL_EXTRACTION % fragment == 0  ) {
                            
                    if( ! lines.isEmpty() )  {
                        resultGeneratedFiles.add(currentFile)     ;
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
                        resultGeneratedFiles.add(currentFile)    ;
                        InOut.createFileIfNotExists(currentFile) ;
                        InOut.writeTextFile(lines, currentFile ) ;
                        lines.clear()                            ;
                        loopForFlush  = 0                        ;
                    }
                }
            }
            
            if( !lines.isEmpty() ) {
                
                if ( loopForFlush >= flushCount )                      {
                      currentFile  =  getCurrentFile ( outputFile      , 
                                                       extension       ,
                                                       ++fragCounter ) ;
                }
                resultGeneratedFiles.add( currentFile )   ;
                InOut.createFileIfNotExists(currentFile)  ;
                InOut.writeTextFile(lines, currentFile )  ;
                lines.clear()                             ;
                loopForFlush  = 0                         ;
                
            }
            
            if ( debug ) debugMode  ( st , query ) ;
            
         } catch ( Exception ex )   {
              
        } finally {
        }

        return new ResultInfo( TOTAL_EXTRACTION , resultGeneratedFiles ) ;
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

   
   private static String getCurrentFile(  String outFile   , 
                                          String extension ,
                                          int fragment     )            {
     
      if ( fragment <= 0 ) {
           return outFile  ; 
      }
    
     return outFile.replace(extension, "") + "_" + fragment + extension ;             
   }    

   private static void debugMode ( OntopOWLStatement st , String query ) {
       
     if( st == null ) return ;
     
     try {
         
         SQLExecutableQuery sqlExecutableQuery = 
                            (SQLExecutableQuery) st.getExecutableQuery(query) ;
         
         String sqlQuery  = sqlExecutableQuery.getSQL()                       ;
         
         System.out.println(" ") ;
         System.out.println("=====================================") ;
         System.out.println(" DEBUG - MODE =======================") ;
         System.out.println("=====================================") ;
         System.out.println(" ") ;

         // Only for debugging purpose, not for 
         // end users: this will redo the query 
         // reformulation, which can be expensive
                 
         System.out.println("============================") ;
         System.out.println("============================") ;
         System.out.println(" The output SQL query :     ") ;
         System.out.println("============================") ;
         System.out.println("                            ") ;
         System.out.println(sqlQuery)                       ;
         System.out.println("                            ") ;
         System.out.println("============================") ;
         System.out.println("============================") ;
         System.out.println("                            ") ;
                    
     } catch (OWLException ex) {
         Logger.getLogger(Processor.class.getName()).log( Level.SEVERE, null, ex ) ;
     }
    
   }
 }
