
*Path : /home/ryahiaoui/Documents/INRA/2021/2021/Présentation_SI/ONTOP/demo/*

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
