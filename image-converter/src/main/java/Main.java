import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.GdxNativesLoader;

public class Main {

    public static void main(String[] args) {
        loadNativeLibraries();
        Pixmap pixmap = new Pixmap(20, 20, Pixmap.Format.RGBA8888);
        System.out.println(pixmap);
    }

    private static void loadNativeLibraries() {
        GdxNativesLoader.load();
    }
}
