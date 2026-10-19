package np.evan.lockscreen;
import org.json.JSONObject;
public final class ShadeJson {
 public static ShadeOptions parse(JSONObject j)throws Exception{ShadeOptions o=new ShadeOptions();o.panelEnabled=j.optBoolean("panelEnabled",true);o.cardsEnabled=j.optBoolean("cardsEnabled",true);o.changeBlur=j.optBoolean("changeBlur",false);o.panelColor=j.optString("panelColor",o.panelColor);o.cardColor=j.optString("cardColor",o.cardColor);o.blurColor=j.optString("blurColor",o.blurColor);o.panelAlpha=j.optInt("panelAlpha",100);o.cardTintAlpha=j.optInt("cardTintAlpha",25);o.cardOpacity=j.optInt("cardOpacity",25);o.blurAlpha=j.optInt("blurAlpha",25);o.blurMode=j.optInt("blurMode",3);o.validate();return o;}
 public static JSONObject json(ShadeOptions o)throws Exception{return new JSONObject().put("panelEnabled",o.panelEnabled).put("cardsEnabled",o.cardsEnabled).put("changeBlur",o.changeBlur).put("panelColor",o.panelColor).put("cardColor",o.cardColor).put("blurColor",o.blurColor).put("panelAlpha",o.panelAlpha).put("cardTintAlpha",o.cardTintAlpha).put("cardOpacity",o.cardOpacity).put("blurAlpha",o.blurAlpha).put("blurMode",o.blurMode);}
}
