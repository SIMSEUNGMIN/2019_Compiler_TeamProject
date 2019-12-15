package listener.main;

import java.util.Hashtable;

import generated.MiniCParser;
import generated.MiniCParser.ExprContext;
import generated.MiniCParser.Fun_declContext;
import generated.MiniCParser.If_stmtContext;
import generated.MiniCParser.Local_declContext;
import generated.MiniCParser.ParamContext;
import generated.MiniCParser.ParamsContext;
import generated.MiniCParser.Type_specContext;
import generated.MiniCParser.Var_declContext;
import listener.main.SymbolTable;
import listener.main.SymbolTable.VarInfo;

public class BytecodeGenListenerHelper {

	// <boolean functions>
	static boolean isFunDecl(MiniCParser.ProgramContext ctx, int i) {
		return ctx.getChild(i).getChild(0) instanceof MiniCParser.Fun_declContext;
	}
    static boolean isFloat(String expr){
        return (expr.contains("f")||expr.contains("F"));
    }
	// type_spec IDENT '[' ']'
	static boolean isArrayParamDecl(ParamContext param) {
		return param.getChildCount() == 4;
	}

	// global vars
	static int initVal(Var_declContext ctx) {
		if(ctx.LITERAL().getText().contains("."))
			initFVal(ctx);
		else
			return Integer.parseInt(ctx.LITERAL().getText());
		return 0;
	}

	static float initFVal(Var_declContext ctx)	{
		return Float.parseFloat(ctx.LITERAL().getText());
	}
	// var_decl	: type_spec IDENT '=' LITERAL ';
	static boolean isDeclWithInit(Var_declContext ctx) {
		return ctx.getChildCount() == 5 ;
	}
	static boolean isIntDecl(Var_declContext ctx)	{
		if(getTypeText(ctx.type_spec()).equals("int"))
			return true;
		return false;
	}
	static boolean isFloatDecl(Var_declContext ctx)	{
		if(getTypeText(ctx.type_spec()).equals("float"))
			return true;
		return false;
	}
	// var_decl	: type_spec IDENT '[' LITERAL ']' ';'
	static boolean isArrayDecl(Var_declContext ctx) {
		return ctx.getChildCount() == 6;
	}

	// <local vars>
	// local_decl	: type_spec IDENT '[' LITERAL ']' ';'
	static int initVal(Local_declContext ctx) {
		if(ctx.LITERAL().getText().contains("."))
			initFVal(ctx);
		else
			return Integer.parseInt(ctx.LITERAL().getText());
		return 0;
	}
	static float initFVal(Local_declContext ctx)	{
		return Float.parseFloat(ctx.LITERAL().getText());
	}
	static boolean isArrayDecl(Local_declContext ctx) {
		return ctx.getChildCount() == 6;
	}
	static boolean isIntDecl(Local_declContext ctx)	{
		if(getTypeText(ctx.type_spec()).equals("int"))
			return true;
		return false;
	}
	static boolean isFloatDecl(Local_declContext ctx)	{
		if(getTypeText(ctx.type_spec()).equals("float"))
			return true;
		return false;
	}
	static boolean isDeclWithInit(Local_declContext ctx) {
		return ctx.getChildCount() == 5;
	}

	//함수의 반환형이 void인지 확인
	static boolean isVoidF(Fun_declContext ctx) {
		String funType = ctx.type_spec().getText();

		if(funType == "void") {
			return true;
		}
		return false;
	}

	static boolean isIntReturn(MiniCParser.Return_stmtContext ctx) {
		return ctx.getChildCount() ==3;
	}


	static boolean isVoidReturn(MiniCParser.Return_stmtContext ctx) {
		return ctx.getChildCount() == 2;
	}

	// <information extraction>
	static String getStackSize(Fun_declContext ctx) {
		return "32";
	}
	static String getLocalVarSize(Fun_declContext ctx) {
		return "32";
	}

	// 타입 확인
	static String getTypeText(Type_specContext typespec) {
		String typeString = typespec.getText();
		return typeString;
	}


	// params
	static String getParamName(ParamContext param) {
		String paramName = param.type_spec().getText();
		return paramName;
	}

	static String getParamTypesText(ParamsContext params) {
		String typeText = "";

		for(int i = 0; i < params.param().size(); i++) {
			MiniCParser.Type_specContext typespec = (MiniCParser.Type_specContext)  params.param(i).getChild(0);
			typeText += getTypeText(typespec); // + ";";
		}
		return typeText;
	}

	// 지역변수 이름 반환
	static String getLocalVarName(Local_declContext local_decl) {
		//지역변수 이름 반환
		String localName = local_decl.IDENT().getText();
		return localName;
	}

	// 함수 이름 반환
	static String getFunName(Fun_declContext ctx) {
		String funName = ctx.children.get(1).getText();
		return funName;
	}

	// 함수 이름 찾음
	static String getFunName(ExprContext ctx) {
		String funName = ctx.children.get(0).getText();
		return funName;
	}

	static boolean noElse(If_stmtContext ctx) {
		return ctx.getChildCount() < 6;
	}

	// 클래스의 처음 생성자형태를 채워서 반환 (전역변수도 있을 때를 고려)
	static String getFunProlog() {
		// return ".class public Test .....
		// ...
		// invokenonvirtual java/lang/Object/<init>()
		// return
		// .end method"
		return "\n" + ".class public Test" + "\n"
				+ ".super java/lang/Object" + "\n" ;
	}

	static String getFunMid() {
		return ".method public <init>()V" + "\n"
				+ ".limit stack 32" + "\n"
				+ "aload_0" + "\n"
				+ "invokenonvirtual java/lang/Object/<init>()V" + "\n";
	}

	static String getFunEnd() {
		return "return" + "\n"
				+ ".end method" + "\n";
	}

	static String getCurrentClassName() {
		return "Test";
	}
}
