import com.android.apksig.ApkSigner;
import com.android.apksig.ApkVerifier;
import java.io.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;
public class SignApk {
 public static void main(String[] args)throws Exception{
  if(args[0].equals("compare")){
   ApkVerifier.Result old=new ApkVerifier.Builder(new File(args[1])).build().verify(),next=new ApkVerifier.Builder(new File(args[2])).build().verify();
   if(!old.isVerified()||!next.isVerified()||old.getSignerCertificates().size()!=next.getSignerCertificates().size())throw new SecurityException("APK signatures could not be compared");
   for(int i=0;i<old.getSignerCertificates().size();i++)if(!Arrays.equals(old.getSignerCertificates().get(i).getEncoded(),next.getSignerCertificates().get(i).getEncoded()))throw new SecurityException("Signing certificate differs");
   System.out.println("Update signing certificate matches the previous APK.");return;
  }
  if(args[0].equals("verify")){
   ApkVerifier.Result result=new ApkVerifier.Builder(new File(args[1])).build().verify();
   System.out.println("Verified="+result.isVerified()+", v1="+result.isVerifiedUsingV1Scheme()+", v2="+result.isVerifiedUsingV2Scheme()+", v3="+result.isVerifiedUsingV3Scheme());
   for(X509Certificate c:result.getSignerCertificates())System.out.println("Certificate SHA-256: "+hex(MessageDigest.getInstance("SHA-256").digest(c.getEncoded())));
   for(Object e:result.getErrors())System.out.println("ERROR "+e);
   if(!result.isVerified())System.exit(1);return;
  }
  // Release builds pass their key through the environment; local builds use the generated development key.
  String alias=env("EVAN_KEY_ALIAS","dev");char[] storePassword=env("EVAN_STORE_PASSWORD","android").toCharArray(),keyPassword=env("EVAN_KEY_PASSWORD",new String(storePassword)).toCharArray();
  KeyStore ks=load(new File(args[2]),storePassword);
  PrivateKey key=(PrivateKey)ks.getKey(alias,keyPassword);if(key==null)throw new KeyStoreException("No private key named "+alias+" in "+args[2]);
  X509Certificate cert=(X509Certificate)ks.getCertificate(alias);
  ApkSigner.SignerConfig config=new ApkSigner.SignerConfig.Builder("evan",key,Collections.singletonList(cert)).build();
  new ApkSigner.Builder(Collections.singletonList(config)).setInputApk(new File(args[0])).setOutputApk(new File(args[1])).setMinSdkVersion(29).setV1SigningEnabled(true).setV2SigningEnabled(true).setV3SigningEnabled(true).build().sign();
 }
 private static String env(String name,String fallback){String v=System.getenv(name);return v==null||v.isEmpty()?fallback:v;}
 private static KeyStore load(File file,char[] password)throws Exception{
  Exception last=null;for(String type:new String[]{"PKCS12","JKS"})try(InputStream in=new FileInputStream(file)){KeyStore ks=KeyStore.getInstance(type);ks.load(in,password);return ks;}catch(Exception e){last=e;}
  throw last;
 }
 private static String hex(byte[] bytes){StringBuilder b=new StringBuilder();for(byte x:bytes){if(b.length()>0)b.append(':');b.append(String.format("%02X",x));}return b.toString();}
}
