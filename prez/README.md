
*Path : /home/ryahiaoui/Documents/INRA/2021/2021/Présentation_SI/ONTOP/demo/*

## Notes :

Présentation ( Suite ) :

Ontop a 2 modes de fonctionnement : 

  - Géneration à la volée de triplets virtuel 
  - Matérialisation 

Avantage :

  - Open source - Communauté réactive
  - Triplets virtuel ( non matérialisés ) - Générés à la volée
  - Synchronisation avec la BD
  - Sparql vers SQL ( entre 6000 et 10000 Lignes SQL ) 
  - Plugin Protegé ( GUI ).
  
Inconvenient : 

   - Charge suplémentaire pour la BD ( dans le cas d'un endpoint Ontop )
   - N'implement pas l'ensemble des fonctionalités SPARQL 1.1 :  https://ontop-vkg.org/guide/compliance.html#sparql-1-1
   - Mapping manuel ( avec Protegé + Plugins Ontop ) :
       * Compétence pour écrire la syntaxe tutrle ( et pas tous les chercheurs ont cette compétence ) 
       * La mise à jours des OBDA peut très vite devenir compliquée ( ajout, suppression, modification de noeuds ) => On devient donc moins productif !
       * Méme si on a une GUI, celle-ci ne dispose pas d'une réprésentation visuelle du travail d'annotation 
   - Perf : endpoint pas perf , pas scalable - Sparql 1.1 pas complet. 
            Materializer : Pas perf + pas sacalable mains raisonneur OK 
   - Pas à l'abris d'erreurs dans les récriture des Requetes SQL : Requetes SQL qui ne marchent pas ( LIMIT - OFFSET, Sub Select..., maintenant "Maybe")
   - Pas de réponses pour certaines requetes SPARQL ( MEME sur la dernière version du CLI ) 
   

Autant sur les perf je pense qu'on aurait pue faire un compromis.. Mais quand on a des Mapping qui ne passent pas à cause d'une mauvaise réeriture des requetes SQL, là ça devient carrément bloquant ! C'est ce qui nous a amené à developper : DataRiv 


---

## Demo Time :

### I. Sparql Endpoint :
 
  #### 1. Ontop CLI ( **/home/ryahiaoui/Documents/INRA/2021/2021/Présentation_SI/ONTOP/demo/ontop_cli/cli/ontop-cli-4.1.1/** )

```
       ./ontop endpoint  --db-password yahiaoui                                 \
                         --db-url jdbc:postgresql://127.0.0.1/foret             \
                         -m ontology_mapping/0_mapping_CSV_Transpiration_0.obda \
                         -t ontology_mapping/ontology.owl                       \
                         -u ryahiaoui -p properties.txt --port 5678
```
  
 #### 2. Blazegraph ( **/home/ryahiaoui/Documents/INRA/2021/2021/Présentation_SI/ONTOP/demo/blazegraph** )

```
      java -jar blazegraph_2_1_6.jar
```


#### 3. Test Sparql Queries :

#### 3.1 SELECT SPO :
   
```    
    PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
    SELECT * WHERE    {
      ?sub ?pred ?obj .
    } 
    LIMIT 10

```

#### 3.2 SELECT COUNT :
   
```    

  SELECT ( COUNT( ?s ) AS ?TOTAL )

  WHERE  {

         ?s ?p ?o .
    } 

```

#### 3.3. CONTRUCT & DUPLICATES !

```

CONSTRUCT { 

  ?s <http://ecoinformatics.org/oboe/oboe.1.2/oboe-core.owl#measurementFor> ?o .
}

WHERE {

   SELECT  ?s  ?o  WHERE {
     
    ?s ?p ?o .
     
     FILTER ( str(?o ) = "http://foret/obs/categ/21/1" ) . 
   
   } LIMIT 10
    
}

```

#### 3.4. SPARQL QUERY USED IN A REAL USE CAS : 
 
```
    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
    PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
    PREFIX : <http://opendata.inra.fr/anaeeOnto#>  
    PREFIX oboe-core: <http://ecoinformatics.org/oboe/oboe.1.2/oboe-core.owl#> 
    PREFIX oboe-temporal: <http://ecoinformatics.org/oboe/oboe.1.2/oboe-temporal.owl#>
    PREFIX oboe-standard: <http://ecoinformatics.org/oboe/oboe.1.2/oboe-standards.owl#>

    SELECT * 

    WHERE {       

               ?obs_var_1 a oboe-core:Observation ; 
                            oboe-core:ofEntity ?anyVariable ; 
                            oboe-core:hasMeasurement ?measu_2 ; 
                            :hasVariableContext ?obs_variable_4  .              


               ?obs_variable_4 a oboe-core:Observation ; 
                                 oboe-core:ofEntity :Variable ; 
                                 oboe-core:hasMeasurement ?measu_5 ; 
                                 oboe-core:hasContext ?obs_categ_7 ;
                                 :informationSystemName ?ISName    ;
                                 :graphClassName ?graphClassName   .
    }

    LIMIT 10000

```

#### 3.4. FULL SYNTHESIS SPARQL QUERY :
 
```
    # Blazegraph :
    
    https://gist.github.com/rac021/58d31dff6f757dc6f09b8672b86eb386
    
```

```
    # Adapted For ONTOP 
    
    https://gist.github.com/rac021/74a1b5f62b54245e601dcaf8ba558ea5

```

## II. MATERIALIZER :

#### 1. ONTOP ( **/home/ryahiaoui/Documents/INRA/2021/2021/Présentation_SI/ONTOP/demo/ontop_cli/cli/ontop-cli-4.1.1/** )
 
```
   ./ontop materialize --format  ntriples --db-password yahiaoui              \
                       --db-url jdbc:postgresql://127.0.0.1/foret             \
                       -m ontology_mapping/0_mapping_CSV_Transpiration_0.obda \
                       -t ontology_mapping/ontology.owl                       \
                       -o data.nt -u ryahiaoui -p properties.txt
```

#### 2 COBY PIPELINE ( **/home/ryahiaoui/Documents/INRA/2021/2021/Présentation_SI/ONTOP/demo/coby_bin/pipeline/orchestrators** )
 
```
   time ./coby.sh login=admin extract="DATA" query=" SI = SI OBS FORET ALL &  year = 2001 & CLASS = flux semi-horaire" job="admin"
   
   time ./coby.sh login=admin extract="SYNTHESIS" query=" SI = SI OBS FORET ALL &  year = 2001 & CLASS = flux semi-horaire" job="admin"
   
```

#### 3 NEW COBY PIPELINE : NetBeans + 
 ( **/home/ryahiaoui/Téléchargements/coby/COBY_1.8_01_04_2021/07-05-2021/coby/GIT_LAB/COBY_05_07_2021_/coby/coby_bin/pipeline/SI/FORET_ALL/output/02_data/** )
 
```
    Materializer.java

```
 
 
