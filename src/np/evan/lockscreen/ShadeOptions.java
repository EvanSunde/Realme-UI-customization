package np.evan.lockscreen;
import java.util.*;
/** Only the resource controls exercised on the owner's firmware. */
public final class ShadeOptions {
 public static final String PANEL="notification_scrim_bg_mask_color",TINT="notification_material_background_color",BASE="notification_material_background_color_compass",BLUR="notification_material_blur_bg";
 public static final Set<String> KEYS=Collections.unmodifiableSet(new HashSet<>(Arrays.asList(PANEL,TINT,BASE,BLUR)));
 public boolean panelEnabled=true,cardsEnabled=true,changeBlur=false;
 public String panelColor="#80CBC4",cardColor="#B9F6CA",blurColor="#00E676";
 public int panelAlpha=100,cardTintAlpha=25,cardOpacity=25,blurAlpha=25,blurMode=3;
 public void validate(){for(String c:new String[]{panelColor,cardColor,blurColor})if(c==null||!c.matches("#[0-9a-fA-F]{6}"))throw new IllegalArgumentException("Choose a six-digit RGB colour");for(int a:new int[]{panelAlpha,cardTintAlpha,cardOpacity,blurAlpha})if(a<0||a>100)throw new IllegalArgumentException("Alpha must be between 0 and 100");if(blurMode < -1||blurMode>3)throw new IllegalArgumentException("Choose a supported blur mode");}
 public static String argb(String rgb,int percent){return String.format(Locale.US,"#%02X%s",Math.round(percent*255f/100f),rgb.substring(1).toUpperCase(Locale.US));}
 public Map<String,String> colors(){validate();Map<String,String> c=new LinkedHashMap<>();if(panelEnabled)c.put(PANEL,argb(panelColor,panelAlpha));if(cardsEnabled){c.put(TINT,argb(cardColor,cardTintAlpha));c.put(BASE,argb("#FAFAFA",cardOpacity));c.put(BLUR,argb(blurColor,blurAlpha));}return c;}
}
