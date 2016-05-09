<h4>ontop-materializer based on version 1.17.1</h4>
===============

Materialize triples from database using : Ontop-Quest API + ODBA file + Ontology.

Steps : 

 1- mvn clean install assembly:single
 
 2- Args :
 
   `-owl  :  owl path file ( Required ) ` 
    
   `-odba :  Mapping file ( Required ) ` 
    
   `-out  :  output path file ( Required ) ` 
    
   `-q   :  Sparql Query ( Not Required ) Default "SELECT DISTINCT ?S ?P ?O { ?S ?P ?O . } " `
    
  3- Exp    :
  
     java -Xms1024M -Xmx2048M -cp ontop-materializer-1.17.0-jar-with-dependencies.jar ontop.Main_1_17 \
     -owl 'ontology.owl' \
     -obda 'ontology.obda' \
     -out 'ontopMaterializedTriples.nt' \
     -q " SELECT ?S ?P ?O { ?S ?P ?O } "
    
     java -Xms1024M -Xmx2048M -cp ontop-materializer-1.17.0-jar-with-dependencies.jar ontop.Main_1_17 \
     -owl 'ontology.owl' \
     -obda 'ontology.obda' \
     -out 'ontopMaterializedTriples.nt'
     
     
    
