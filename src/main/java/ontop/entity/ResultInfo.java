
 package ontop.entity ;

 import java.util.LinkedHashSet ;

/**
 *
 * @author ryahiaoui
 */
public class ResultInfo {
 
    private final int               Total          ;
    private final LinkedHashSet<String> GeneratedFiles ;

    public ResultInfo( int total , LinkedHashSet<String> generated_files ) {
        this.Total          = total           ;
        this.GeneratedFiles = generated_files ;
    }

    public int getTotal() {
        return Total;
    }

    public LinkedHashSet <String> getGeneratedFiles() {
        return GeneratedFiles ;
    }

 
    
}
