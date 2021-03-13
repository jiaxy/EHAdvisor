package org.example;

import com.github.javaparser.JavaParser;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.*;
import java.util.*;

/**
 * Hello world!
 *
 *
 */
public class App 
{
    public static class Method{
        public String methodName;
        public String comment;
        public String para;
    }

    static List<Method> methods = new ArrayList<Method>();
    public static void main( String[] args ) throws FileNotFoundException, IOException, IllegalArgumentException, IllegalAccessException{
        String path = "29project";
        File file = new File(path);
        File[] fs = file.listFiles();
        for (File f : fs ) {
            GetFile(f);

            exportCsv(f.getName(),methods);
            methods.clear();
        }

        System.out.println("end");
    }

    public static void GetFile( File file)throws FileNotFoundException {
        File[] fs = file.listFiles();
        for (File f : fs) {
            if(f.isDirectory()){
                GetFile(f);
            }
            if (f.toString().endsWith(".java")){
                try {
                    System.out.println(f);
                    FileInputStream in = new FileInputStream(f.toString());
                    CompilationUnit cu = StaticJavaParser.parse(in);
                    new MethodVisitor().visit(cu, null);
                }
              catch (Exception e){

              }
            }

        }
        return ;

    }
    public static <T> String exportCsv(String FileName,List<Method> methods) throws IOException, IllegalArgumentException, IllegalAccessException{
        File file = new File("G:\\exceptionPaper\\comment\\comment\\"+FileName+".csv");

        OutputStreamWriter ow = new OutputStreamWriter(new FileOutputStream(file), "gbk");

        for(Method method : methods){

            ow.write(method.methodName);
            ow.write(",");
            ow.write(method.para);
            ow.write(",");
            ow.write(method.comment);
            ow.write(",");
            ow.write("\r\n");


        }
        ow.flush();
        ow.close();
        return "0";
    }
    private static class MethodVisitor extends VoidVisitorAdapter {
        @Override
        public void visit(MethodDeclaration n, Object arg) {
            // here you can access the attributes of the method.
            // this method will be called for all methods in this
            // CompilationUnit, including inner class methods
            Method method = new Method();
            String res="";
            for (Parameter p :n.getParameters()){
                res=res+p.getTypeAsString()+"|";
                res= res+p.getNameAsString()+"$";
            }

            method.methodName=n.getName().asString();
            method.methodName = method.methodName.replace('\n',' ');
            method.methodName = method.methodName.replace('\r',' ');
            method.methodName = method.methodName.replace(',',' ');
            method.comment =n.getComment().isPresent() ? n.getComment().get().toString() :"" ;
//            if (method.comment!="")
//                System.out.println("1");
            method.comment = method.comment.replace('\n',' ');
            method.comment = method.comment.replace('\r',' ');
            method.comment = method.comment.replace(',',' ');
            method.comment = method.comment.replace('*',' ');
            method.comment = method.comment.replace('/',' ');
            method.comment = method.comment.replace('>',' ');
            res = res.replace('\n',' ');
            res = res.replace('\r',' ');
            res = res.replace(',',' ');
            method.para = res;
            methods.add(method) ;
        }


    }
}
