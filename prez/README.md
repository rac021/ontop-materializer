
<sup>**Présentation en 2 parties**</sup> 

* <sup>**Partie 1 :** Ancienne presentation ( 2017 ) , apporter des mises à jour sur les dernières version d'Ontop ( materializer + enpoint ) au fur de l'avancement</sup>

* <sup>**Partie 2 :** Démo : Comparaison Ontop - Blazegraph - Pipeline Coby</sup>

*Prez_Link : https://forgemia.inra.fr/anaee-dev/coby/-/blob/master/docs/coby_prez/Envi_Pipeline_Coby.pdf*

*Local_Path* : **/home/ryahiaoui/Documents/INRA/2021/2021/Présentation_SI/ONTOP/**

## Notes :

Présentation ( Suite ) :

### Résumé avant la Démo : 

Ontop a 2 modes de fonctionnement : 

  - Endpoint : Géneration à la volée de triplets virtuel afin de répondre à une requete SPARQL
  - Le materializer : Matérialisation des triplets sur disque 

Avantage :

  - Open source - Communauté réactive
  - Ontop Endpoint : Triplets virtuel ( non matérialisés ) - Générés à la volée
  - Ontop Endpoint : Synchronisation avec la BD
  - Sparql vers SQL ( entre 6000 et 10000 Lignes SQL ) 
  - Plugin Protegé ( GUI ) : Validation des entités ( Concepts ) mais pas des requetes SQL
  - Ontop Materializer supporte le Streaming depuis 2020
  - Raisonneur QUEST de plus en plus mature
  
Inconvenient : 

   - Charge suplémentaire pour la BD ( dans le cas d'un endpoint Ontop )
   - N'implemente pas l'ensemble des fonctionalités SPARQL 1.1 :  https://ontop-vkg.org/guide/compliance.html#sparql-1-1
   - Mapping manuel ( avec Protegé + Plugins Ontop ) :
       * Compétence pour écrire la syntaxe tutrle ( et pas tous les chercheurs ont cette compétence ) 
       * La mise à jours des OBDA peut très vite devenir compliquée ( ajout, suppression, modification de noeuds ) => On devient donc moins productif !
       * Méme si on a une GUI, celle-ci ne dispose pas d'une réprésentation visuelle du travail d'annotation 
   - Perf : Endpoint pas perf , pas scalable : Fonctionnement identique avec petite ou grosse machine. 
            Materializer : Méme s'il supporte le Streaming, il n'est pas perf ( pas sacalable ) : Fonctionnement identique avec petite ou grosse machine. 
   - Pas à l'abris d'erreurs dans les récriture des Requetes SQL : Requetes SQL qui ne marchent pas ( LIMIT - OFFSET, Sub Select..., maintenant "Maybe") https://github.com/ontop/ontop/issues/438
   - Pas de réponses pour certaines requetes SPARQL ( MEME sur la dernière version du CLI ) 
   

Autant sur les perf je pense qu'on aurait pue faire un compromis.. Mais quand on a des Mapping qui ne passent pas à cause d'une mauvaise réeriture des requetes SQL, là ça devient carrément bloquant ! C'est ce qui nous a amené à developper : DataRiv 


---

## Demo Time :

### I. Sparql Endpoint :
 
  #### 1. Ontop CLI <sup>( **/home/ryahiaoui/Documents/INRA/2021/2021/Présentation_SI/ONTOP/demo/ontop_cli/cli/ontop-cli-4.1.1/** )</sup>

```
       ./ontop endpoint  --db-password yahiaoui                                 \
                         --db-url jdbc:postgresql://127.0.0.1/foret             \
                         -m ontology_mapping/0_mapping_CSV_Transpiration_0.obda \
                         -t ontology_mapping/ontology.owl                       \
                         -u ryahiaoui -p properties.txt --port 5678
```
  
 #### 2. Blazegraph <sup>( **/home/ryahiaoui/Documents/INRA/2021/2021/Présentation_SI/ONTOP/demo/blazegraph** )</sup>

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

#### 3.3. CONSTRUCT & DUPLICATES !

```

# DUPLICATE TRIPLES !

CONSTRUCT { 

  ?s <http://ecoinformatics.org/oboe/oboe.1.2/oboe-core.owl#hasMeasurement> ?o .
}

WHERE {

   SELECT  ?s  ?o  WHERE {
     
    ?s <http://ecoinformatics.org/oboe/oboe.1.2/oboe-core.owl#hasMeasurement> ?o .
    ?s <http://ecoinformatics.org/oboe/oboe.1.2/oboe-core.owl#hasContext> ?context .
    
  FILTER (str(?s ) = "http://foret/obs/Transpiration/21/37014306" ) . 
   
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
 
 * ##### Blazegraph :
 
  - https://gist.github.com/rac021/58d31dff6f757dc6f09b8672b86eb386
   

  * ##### Adapted For ONTOP 

  - https://gist.github.com/rac021/74a1b5f62b54245e601dcaf8ba558ea5

---

## II. MATERIALIZER :

#### 1. ONTOP <sup>( **/home/ryahiaoui/Documents/INRA/2021/2021/Présentation_SI/ONTOP/demo/ontop_cli/cli/ontop-cli-4.1.1/** )</sup>
 
```
   time ./ontop materialize --format  ntriples --db-password yahiaoui              \
                           --db-url jdbc:postgresql://127.0.0.1/foret             \
                           -m ontology_mapping/0_mapping_CSV_Transpiration_0.obda \
                           -t ontology_mapping/ontology.owl                       \
                           -o data.nt -u ryahiaoui -p properties.txt
```

#### 2 COBY PIPELINE <sup>( **/home/ryahiaoui/Documents/INRA/2021/2021/Présentation_SI/ONTOP/demo/coby_bin/pipeline/orchestrators** )</sup>
 
```
   # DATA - /var/coby_export/
   time ./coby.sh login=admin extract="DATA" query=" SI = SI OBS FORET ALL &      \
         year = 2001 & CLASS = flux semi-horaire" job="admin" 
   
   # SYNTHESIS
   time ./coby.sh login=admin extract="SYNTHESIS" query=" SI = SI OBS FORET ALL & \
        year = 2001 & CLASS = flux semi-horaire" job="admin" 
   
```

#### 3 NEW COBY PIPELINE : NetBeans + 
 <sup>( **/home/ryahiaoui/Téléchargements/coby/COBY_1.8_01_04_2021/07-05-2021/coby/GIT_LAB/COBY_05_07_2021_/coby/coby_bin/pipeline/SI/FORET_ALL/output/02_data/** )</sup>
 
```
    Materializer.java

```

## III. BENCH : **Full OBDA Mapping - All Variables + All Sites + All Years**

 1- **ONTOP**  ( xmx 32g ) : **192_169_531** Triples ( With Inference + Without Duplicates ) :
    → **118mn** ( ~ 2h ) - 1.3 Gb RAM — **27142 Triples/second** - ( 29.9 GB File Size )
	  → **1_000_000_000** triples ~ **10h30mn**
 
 2- **DATARIV** ( xmx 8g ) : **219_181_348** Triples ( With Inference + Without Duplicates ) : 
    → **05mn25** - 2.5 Gb RAM - **429767 Triples/second** ( 33. GB File size ) ⇒ **FACTEUR 15**
  	→ **1_000_000_000** triples between **24mn & 28 mn** 


----


## Protegé Demo : <sup>/home/ryahiaoui/Documents/INRA/2021/2021/Présentation_SI/ONTOP/demo/protege/Protege-5.5.0</sup>


```

jdbc:postgresql://127.0.0.1/foret

PREFIX : oboe-core : http://ecoinformatics.org/oboe/oboe.1.2/oboe-core.owl#

######

<http://anaee.inra.fr/observation/{id}> a oboe-core:Observation ; 
oboe-core:#hasMeasurement <http://anaee.inra.fr/measurement/{id}> . 

select 1 as id

######

<http://anaee.inra.fr/measurement/{id}> a oboe-core:Measurement ;
oboe-core:#hasValue <http://10> . 

SELECT 1 as id

######

```

---

Le Web des données (linked data, en anglais) est une initiative du W3C (Consortium World Wide Web) visant à favoriser la publication de données structurées sur le Web, non pas sous la forme de silos de données isolés les uns des autres, mais en les reliant entre elles pour constituer un réseau global d'informations.


---

LIMITATION : BLAZEGRAPH CLUSTERING, RDFOX...
