<h4>ontop-materializer based on version 1.17.1</h4>
===============

Materialize triples from database using : Ontop-Quest API + ODBA file + Ontology.

Steps : 

 1 - `mvn clean install assembly:single`
 
 2 - Args 
 
   `-owl  :  owl path file ( Required ) ` 
    
   `-odba :  Mapping file ( Required ) ` 
    
   `-out  :  output path file ( Required ) ` 
    
   `-q    :  Sparql Query ( Not Required ) Default "SELECT DISTINCT ?S ?P ?O { ?S ?P ?O . } " `
   
   `-ttl  :  Activate output turtle format ( Not Required ). Default : FALSE ` 
              
 - If -ttl is specified, Query must contains exactly 3 variables  ( S, P, O ). 
   
3 - Exp 
  
     java  -Xms1024M -Xmx2048M -cp ontop-materializer-1.17.0-jar-with-dependencies.jar ontop.Main_1_17 \
     -owl  'ontology.owl'                                                                              \
     -obda 'ontology.obda'                                                                             \
     -out  './ontopMaterializedTriples.nt'                                                             \
     -q    " SELECT ?S ?P ?O { ?S ?P ?O } "                                                            \
     -ttl
     
    # SELECT ALL triples and output in turtle format :
    java  -Xms1 024M -Xmx2048M -cp ontop-materializer-1.17.0-jar-with-dependencies.jar ontop.Main_1_17 \
     -owl  'ontology.owl'                                                                              \
     -obda 'ontology.obda'                                                                             \
     -out  './ontopMaterializedTriples.nt'                                                             

    # Custom Query
    java  -Xms1024M -Xmx2048M -cp ontop-materializer-1.17.0-jar-with-dependencies.jar ontop.Main_1_17  \
     -owl  'ontology.owl'                                                                              \
     -obda 'ontology.obda'                                                                             \
     -out  './ontopMaterializedTriples.nt'                                                             \
     -q    " SELECT ?uri ?name ?location ?aria WHERE { .... } "                                        \

    # Error in the following example ( because -ttl is specified and number of parameters in Query != 3 ) 
    java  -Xms1024M -Xmx2048M -cp ontop-materializer-1.17.0-jar-with-dependencies.jar ontop.Main_1_17  \
     -owl  'ontology.owl'                                                                              \
     -obda 'ontology.obda'                                                                             \
     -out  './ontopMaterializedTriples.nt'                                                             \
     -q    " SELECT ?uri ?name ?location ?aria WHERE { .... } "                                        \
     -ttl
     
