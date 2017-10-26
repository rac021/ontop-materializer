<h4>Ontop-materializer Version 3.0.0 </h4>
-----------------

| Branch    | build status  |
|-----------|---------------|
| [V3](https://github.com/rac021/ontop-matarializer/tree/V3)  |[![Build Status](https://travis-ci.org/ontop/ontop.svg?branch=master)](https://github.com/rac021/ontop-matarializer/tree/V3)|

Materialize triples from database using : Ontop-Quest API + OBDA file + Ontology .

Steps : 

 **1** - `mvn clean install assembly:single`
 
 **2   - Args**
 
   `-owl  :  owl path file. ( Required ) ` 
    
   `-odba :  Mapping file. ( Required ) ` 
    
   `-out  :  output path file. ( Required ) ` 
    
   `-q    :  Sparql Query. ( Not Required ) Default "SELECT DISTINCT ?S ?P ?O { ?S ?P ?O . } " `
   
   `-ttl  :  Activate output turtle format. ( Not Required ). Default : FALSE ` 

   *Note: if -ttl is specified, Query must contain exactly 3 variables  ( S, P, O ) .*
    
**3 - Exp**
  
 ```
 ❯  java  -Xmx2048M -cp target/ontop-materializer-1.18.0-jar-with-dependencies.jar ontop.Main_1_18 \
    -owl  'src/main/resources/mapping/ontology.owl'                                                \
    -obda 'src/main/resources/mapping/ontology.obda'                                               \
    -out  './ontopMaterializedTriples.nt'                                                          \
    -q    " SELECT ?S ?P ?O { ?S ?P ?O } "                                                         \
    -ttl
 ```
 
 # SELECT ALL triples and output in turtle format :
     
```
 ❯  java  -Xmx2048M -cp target/ontop-materializer-1.18.0-jar-with-dependencies.jar ontop.Main_1_18 \
    -owl  'src/main/resources/mapping/ontology.owl'                                                \
    -obda 'src/main/resources/mapping/ontology.obda'                                               \
    -out  './ontopMaterializedTriples.nt'                                                          \
 ```
 # Custom Query :
       
```
 ❯  java  -Xmx2048M -cp target/ontop-materializer-1.18.0-jar-with-dependencies.jar ontop.Main_1_18 \
    -owl  'src/main/resources/mapping/ontology.owl'                                                \
    -obda 'src/main/resources/mapping/ontology.obda'                                               \
    -out  './ontopMaterializedTriples.nt'                                                          \
    -q    " SELECT ?uri ?name ?location ?aria WHERE { .... } " 
 ```
 
 # Error in the following exp  (  because  -ttl  is specified and number of variables in Query != 3 ) 
    
 ```
 ❯  java  -Xmx2048M -cp target/ontop-materializer-1.18.0-jar-with-dependencies.jar ontop.Main_1_18 \
    -owl  'src/main/resources/mapping/ontology.owl'                                                \
    -obda 'src/main/resources/mapping/ontology.obda'                                               \
    -out  './ontopMaterializedTriples.nt'                                                          \
    -q    " SELECT ?uri ?name ?location ?aria WHERE { .... } "                                     \
    -ttl
 ```
