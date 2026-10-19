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
   for(Object e:result.getErrors())System.out.println("ERROR "+e);
   if(!result.isVerified())System.exit(1);return;
  }
  KeyStore ks=KeyStore.getInstance("JKS");try(InputStream in=new FileInputStream(args[2])){ks.load(in,"android".toCharArray());}
  PrivateKey key=(PrivateKey)ks.getKey("evan-debug","android".toCharArray());
  X509Certificate cert=(X509Certificate)ks.getCertificate("evan-debug");
  ApkSigner.SignerConfig config=new ApkSigner.SignerConfig.Builder("evan-debug",key,Collections.singletonList(cert)).build();
  new ApkSigner.Builder(Collections.singletonList(config)).setInputApk(new File(args[0])).setOutputApk(new File(args[1])).setMinSdkVersion(29).setV1SigningEnabled(true).setV2SigningEnabled(true).setV3SigningEnabled(true).build().sign();
 }
}
