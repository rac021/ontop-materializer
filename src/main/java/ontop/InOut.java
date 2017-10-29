
 package ontop ;

 import java.io.File ;
 import java.util.List ;
 import java.nio.file.Path ;
 import java.io.IOException ;
 import java.nio.file.Files ;
 import java.nio.file.Paths ;
 import java.util.Collection ;
 import java.nio.file.LinkOption ;
 import java.nio.file.StandardOpenOption ;
 import java.nio.charset.StandardCharsets ;

 /**
 *
 * @author ryahiaoui
 */
 public class InOut {
    
    public static List<String> readTextFile( String fileName ) throws IOException {
      Path path = Paths.get(fileName)                          ;
      return Files.readAllLines(path, StandardCharsets.UTF_8 ) ;
    }

    public static void writeTextFile(List<String> strLines, String fileName ) throws IOException {
      Path path = Paths.get(fileName) ;
      Files.write(path, strLines, StandardCharsets.UTF_8,  StandardOpenOption.APPEND) ;
    }
    
    public static void writeTextFile( String text, String fileName ) throws IOException {
      Path path = Paths.get(fileName)      ;
      Files.write( path, text.getBytes()) ;
    }
    
    public static void deleteAndCreateFile(String path ) throws IOException {
       
       String directory = path.substring(0 , path.lastIndexOf("/")) ;
       Path pat = Paths.get(path) ;
       boolean exists = Files.exists(pat, new LinkOption[]{ LinkOption.NOFOLLOW_LINKS}) ;
       
       if(!exists) {
          checkOrCreateDirectory (directory) ;
       }
       else {
           deleteFile(path) ;
       }
       
       createFile(path) ;
    }
    
    public static void createFileIfNotExists(String path ) throws IOException {
       
       Path pat = Paths.get(path) ;
       boolean exists = Files.exists(pat, new LinkOption[]{ LinkOption.NOFOLLOW_LINKS}) ;
       
       if(!exists) {
          createFile(path) ;
       }
    }

    private static void checkOrCreateDirectory ( String directory ) throws IOException {
      
      Path path = Paths.get(directory) ;
       if(!Files.exists(path, new LinkOption[]{ LinkOption.NOFOLLOW_LINKS}))
       Files.createDirectory(path) ;
    }
    
    private static void createFile( String path ) throws IOException {
      File file = new File(path) ;
      file.createNewFile()       ;
    }
    
    private static void deleteFile( String path ) throws IOException {
      Path pat = Paths.get(path) ;
      Files.delete(pat)          ;
    }
    
    public static String extractFolder( String path )  {
       File file = new File(path)                ;
       if(file.isDirectory()) return path        ;
       return file.getAbsoluteFile().getParent() + "/" ;
    }
    
    public static boolean existFile( String path ) {
      
    if( path == null ) return false ;
    Path pat = Paths.get( path )    ;
    return Files.exists( pat, new LinkOption[]{ LinkOption.NOFOLLOW_LINKS}) ;
      
    }

    static void removeFile(String out) {
        new File(out).delete()         ;
    }
    
    static void removeFiles( Collection<String> out) {
       out.forEach( file -> removeFile(file ))       ;
    }
    
    static void removeFilesStartsWith( String folder , String fileName )   {
        
        try {
            Files.list(Paths.get(folder))
                 .forEach( file -> {
                   if (file.getFileName().toString().startsWith(fileName)) {
                     removeFile( file.toAbsolutePath().toString() )  ;
                   }
                 }) ;
        } catch (IOException ex) {
        }
    }
    
    public static String getfileName(String outputFile) {
         Path path = Paths.get(outputFile)    ;
         return path.getFileName().toString() ;
    }    
      
    public static String getFileExtension( String fileName ) {      
        if(fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0 )
        return fileName.substring(fileName.lastIndexOf(".") ) ;
        else return "" ;
    }
     
    public static String getFileWithoutExtension( String fileName ) {      
         return fileName.replaceFirst("[.][^.]+$", "") ;
    }
   
    public static String getFolder(String outputFile ) {
      Path path = Paths.get(outputFile)  ;
      return path.getParent().toString() ;
    }
 }
