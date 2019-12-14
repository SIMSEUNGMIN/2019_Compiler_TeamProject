package listener.main;

import java.util.Hashtable;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;

import generated.MiniCBaseListener;
import generated.MiniCParser;
import generated.MiniCParser.ExprContext;
import generated.MiniCParser.Fun_declContext;
import generated.MiniCParser.Local_declContext;
import generated.MiniCParser.ParamsContext;
import generated.MiniCParser.ProgramContext;
import generated.MiniCParser.StmtContext;
import generated.MiniCParser.Type_specContext;
import generated.MiniCParser.Var_declContext;

import static listener.main.BytecodeGenListenerHelper.*;
import static listener.main.SymbolTable.*;
import static listener.main.SymbolTable.Type.FLOAT;
import static listener.main.SymbolTable.Type.INT;

public class BytecodeGenListener extends MiniCBaseListener implements ParseTreeListener {
	ParseTreeProperty<String> newTexts = new ParseTreeProperty<String>();
	SymbolTable symbolTable = new SymbolTable();

	int tab = 0;
	int label = 0;

	// program	: decl+
	@Override
	public void enterFun_decl(MiniCParser.Fun_declContext ctx) {
		symbolTable.initFunDecl();

		String fname = getFunName(ctx);
		ParamsContext params;

		//메인일 경우와 메인이 아닌 경우를 구분
		if (fname.equals("main")) {
			symbolTable.putLocalVar("args", Type.INTARRAY);
		} else {
			symbolTable.putFunSpecStr(ctx);
			//함수에 들어가는 파라미터
			params = (MiniCParser.ParamsContext) ctx.getChild(3);
			symbolTable.putParams(params);
		}
	}


	// var_decl	: type_spec IDENT ';' | type_spec IDENT '=' LITERAL ';'|type_spec IDENT '[' LITERAL ']' ';'
	@Override // 전역
	public void enterVar_decl(MiniCParser.Var_declContext ctx) {
		String varName = ctx.IDENT().getText();

		if (isArrayDecl(ctx)) {
			symbolTable.putGlobalVar(varName, Type.INTARRAY);
		}
		else if (isDeclWithInit(ctx)) {
            if(isIntDecl(ctx)){
                symbolTable.putGlobalVarWithInitVal(varName, INT, initVal(ctx));
            }else if(isFloatDecl(ctx)){
                symbolTable.putGlobalVarWithInitVal(varName, FLOAT, initVal(ctx));
            }
		}
		else  { // simple decl
            if(isIntDecl(ctx)){
                symbolTable.putGlobalVar(varName, INT);
            }else if(isFloatDecl(ctx)){
                symbolTable.putGlobalVar(varName, FLOAT);
            }
		}
	}


	@Override // 지역
	public void enterLocal_decl(MiniCParser.Local_declContext ctx) {
		String expr = "";
		if (isArrayDecl(ctx)) {
			symbolTable.putLocalVar(getLocalVarName(ctx), Type.INTARRAY);
		}
		else if (isDeclWithInit(ctx)) { // 선언도 포함
            if(isIntDecl(ctx)){
                symbolTable.putLocalVarWithInitVal(getLocalVarName(ctx), INT, initVal(ctx));
            }else if(isFloatDecl(ctx)){
                symbolTable.putLocalVarWithInitVal(getLocalVarName(ctx), FLOAT, initVal(ctx));
            }
		}
		else  { // simple decl
            if(isIntDecl(ctx)){
                symbolTable.putLocalVar(getLocalVarName(ctx), INT);
            }else if(isFloatDecl(ctx)){
                symbolTable.putLocalVar(getLocalVarName(ctx), FLOAT);
            }
		}
		newTexts.put(ctx, expr);
	}


	@Override
	public void exitProgram(MiniCParser.ProgramContext ctx) {
		String classProlog = getFunProlog();
		String classEnd = getFunEnd();

		String fun_decl = "", var_decl = "";

		for(int i = 0; i < ctx.getChildCount(); i++) {
			if(isFunDecl(ctx, i))
				fun_decl += newTexts.get(ctx.decl(i));
			else
				var_decl += newTexts.get(ctx.decl(i));
		}

		newTexts.put(ctx, classProlog + var_decl + classEnd + fun_decl);

		System.out.println(newTexts.get(ctx));

	}


	// decl	: var_decl | fun_decl
	@Override
	public void exitDecl(MiniCParser.DeclContext ctx) {
		String decl = "";
		if(ctx.getChildCount() == 1)
		{
			if(ctx.var_decl() != null)		//var_decl
				decl += newTexts.get(ctx.var_decl());
			else							//fun_decl
				decl += newTexts.get(ctx.fun_decl()) + ".end method" + "\n";
		}
		newTexts.put(ctx, decl);
	}

	// stmt	: expr_stmt | compound_stmt | if_stmt | while_stmt | return_stmt
	@Override
	public void exitStmt(MiniCParser.StmtContext ctx) {
		String stmt = "";
		if(ctx.getChildCount() > 0)
		{
			if(ctx.expr_stmt() != null)				// expr_stmt
				stmt += newTexts.get(ctx.expr_stmt());
			else if(ctx.compound_stmt() != null)	// compound_stmt
				stmt += newTexts.get(ctx.compound_stmt());
			else if(ctx.if_stmt() != null)			// if_stmt
				stmt += newTexts.get(ctx.if_stmt());
			else if(ctx.while_stmt() != null)		// while_stmt
				stmt += newTexts.get(ctx.while_stmt());
			else if(ctx.return_stmt() != null)		// return_stmt
				stmt += newTexts.get(ctx.return_stmt());
			else; 									// 예상 외의 입력이 들어온 경우
		}
		newTexts.put(ctx, stmt);
	}

	// expr_stmt	: expr ';'
	@Override
	public void exitExpr_stmt(MiniCParser.Expr_stmtContext ctx) {
		String stmt = "";
		if(ctx.getChildCount() == 2)
		{
			stmt += newTexts.get(ctx.expr());	// expr
		}
		newTexts.put(ctx, stmt);
	}


	// while_stmt	: WHILE '(' expr ')' stmt
	@Override
	public void exitWhile_stmt(MiniCParser.While_stmtContext ctx) {

		String lend = symbolTable.newLabel();
		String loop = symbolTable.newLabel();

		String expr = newTexts.get(ctx.expr());
		String stmt = newTexts.get(ctx.stmt());

		// 집어넣을 문장을 만듦
		String whileString = loop + ": " + "\n" // 루프 시작
				+ expr + "\n" //루프의 조건문
				+ "ifeq " + lend + "\n" // 만약 틀리다면 lend로 감
				+ "ldc 1" + "\n" // 맞을 경우 ldc 1
				+ stmt // 반복문 안의 문장 수행
				+ "goto " + loop + "\n" // 다시 루프로 돌아감
				+ lend +" : " + "\n";

		newTexts.put(ctx, whileString);
	}

	// type_spec IDENT '(' params ')' compound_stmt
	@Override
	public void exitFun_decl(MiniCParser.Fun_declContext ctx) {
		//함수의 헤더를 붙이고 함수 안의 내용을 newTexts에서 불러옴
		newTexts.put(ctx, funcHeader(ctx, ctx.IDENT().getText()) + newTexts.get(ctx.compound_stmt()));
	}


	private String funcHeader(MiniCParser.Fun_declContext ctx, String fname) {
		return ".method public static " + symbolTable.getFunSpecStr(fname) + "\n"
				+ ".limit stack " + getStackSize(ctx) + "\n"
				+ ".limit locals " + getLocalVarSize(ctx) + "\n"
				+ "aload_0" + "\n";

	}

	@Override //전역 변수
	public void exitVar_decl(MiniCParser.Var_declContext ctx) {
		String thisString = "aload_0" + "\n";
		String varName = ctx.IDENT().getText();
		String varValue = ctx.LITERAL().getText();
		String varDecl = "";

		//초기화 값이 있을 때만 만들어준다
		if (isDeclWithInit(ctx)) {
			//varDecl += "putfield " + varName + "\n";

			//이때 변수의 값이 6을 넘으면 bipush를 해야한다
			if(Integer.parseInt(varValue) >= 6) {
				varDecl += thisString +
						"bipush " + varValue + "\n" +
						"putfield " + "Test/" + varName + " " + "I" + "\n";
			}
			else {
				varDecl += thisString +
						"iconst_" + varValue + "\n" +
						"putfield " + "Test/" + varName + " " + "I" + "\n";
			}
			// v. initialization => Later! skip now..:
		}
		newTexts.put(ctx, varDecl);
	}


	@Override
	public void exitLocal_decl(MiniCParser.Local_declContext ctx) {
		String varDecl = "";
		if (isDeclWithInit(ctx)) {
            if(isIntDecl(ctx)) {
                symbolTable.putLocalVarWithInitVal(getLocalVarName(ctx), INT, initVal(ctx));
                String vId = symbolTable.getVarId(ctx);
                varDecl += "ldc " + ctx.LITERAL().getText() + "\n"
                        + "istore_" + vId + "\n";
            }else if(isFloatDecl(ctx)){
                symbolTable.putLocalVarWithInitVal(getLocalVarName(ctx), FLOAT, initVal(ctx));
                String vId = symbolTable.getVarId(ctx);
                varDecl += "ldc " + ctx.LITERAL().getText() + "\n"
                        + "fstore_" + vId + "\n";

            }
		}
		else if(isArrayDecl(ctx)){
            String vId = symbolTable.getVarId(ctx);
            String reset = "";
            for(int i = 0; i<Integer.parseInt(ctx.getChild(3).getText()); i++)  {
                reset += ("dup\n"
                        +"iconst_"+i+"\n"
                        +"iconst_0\n"
                        +"iastore\n");
            }
            varDecl += "iconst_"+ ctx.getChild(3)+"\n"
                    +"newarray\tint\n"
                    +reset+"astore_"+vId+"\n";
        }
		newTexts.put(ctx, varDecl);
	}


	// compound_stmt	: '{' local_decl* stmt* '}'
	@Override
	public void exitCompound_stmt(MiniCParser.Compound_stmtContext ctx) {
		String localDecl = "";
		String nextStmt = "";

		//local_decl* stmt* 이므로 개수만큼 반복문을 수행
		for(int i = 0; i < ctx.local_decl().size(); i++) {
			localDecl += newTexts.get(ctx.local_decl(i));
		}

		for(int i = 0; i < ctx.stmt().size(); i++) {
			nextStmt += newTexts.get(ctx.stmt(i));
		}

		newTexts.put(ctx, localDecl + nextStmt);
	}

	// if_stmt	: IF '(' expr ')' stmt | IF '(' expr ')' stmt ELSE stmt;
	@Override
	public void exitIf_stmt(MiniCParser.If_stmtContext ctx) {
		String stmt = "";
		String condExpr= newTexts.get(ctx.expr());
		String thenStmt = newTexts.get(ctx.stmt(0));

		String lend = symbolTable.newLabel();
		String lelse = symbolTable.newLabel();


		if(noElse(ctx)) {	// if 만 있을 경우
			stmt += condExpr + "\n"
					+ "ifeq " + lend + "\n"
					+ thenStmt
					+ lend + ":"  + "\n";
		}
		else {
			String elseStmt = newTexts.get(ctx.stmt(1));
			stmt += condExpr + "\n"
					+ "ifeq " + lelse + "\n"
					+ thenStmt + "\n"
					+ "goto " + lend + "\n"
					+ lelse + ": " + elseStmt + "\n"
					+ lend + ":"  + "\n";
		}

		newTexts.put(ctx, stmt);
	}


	// return_stmt	: RETURN ';' | RETURN expr ';'
	@Override
	public void exitReturn_stmt(MiniCParser.Return_stmtContext ctx) {
		// return ; 인 경우
		if(ctx.getChildCount() == 2) {
			newTexts.put(ctx, ctx.RETURN().getText() + "\n");
		}
		// return expr ; 인 경우
		else {
			newTexts.put(ctx, newTexts.get(ctx.expr()) + "i" + ctx.RETURN().getText() + "\n");
		}
	}


	@Override
	public void exitExpr(MiniCParser.ExprContext ctx) {
		String expr = "";
        String idName = "";
		if(ctx.getChildCount() <= 0) {
			newTexts.put(ctx, "");
			return;
		}
        System.out.println(ctx.getText());
		if(ctx.getChildCount() == 1) { // IDENT | LITERAL
			if(ctx.IDENT() != null) {
				idName = ctx.IDENT().getText();
				if(!symbolTable.isLocal(idName)&&(symbolTable.getVarType(idName) == INT)) { //IDENT가 전역 변수일 경우
                    expr += "getfield " + "Test/" + idName + " " + "I" + "\n";
                }
				else if(!symbolTable.isLocal(idName)&&(symbolTable.getVarType(idName) == FLOAT)) { //IDENT가 전역 변수일 경우
                    expr += "getfield " + "Test/" + idName + " " + "F" + "\n";
                }
				else if(symbolTable.getVarType(idName) == INT) {
					expr += "iload_" + symbolTable.getVarId(idName) + " \n";
				}else if(symbolTable.getVarType(idName)==Type.INTARRAY){
					expr += "aload_"+symbolTable.getVarId(idName)+"\n";
				}else if(symbolTable.getVarType(idName)== FLOAT){
                    expr += "fload_"+symbolTable.getVarId(idName)+"\n";
                }
				//else	// Type int array => Later! skip now..
				//	expr += "           lda " + symbolTable.get(ctx.IDENT().getText()).value + " \n";
			} else if (ctx.LITERAL() != null) {
				String literalStr = ctx.LITERAL().getText();
				expr += "ldc " + literalStr + " \n";
			}
		} else if(ctx.getChildCount() == 2) { // UnaryOperation
			expr = handleUnaryExpr(ctx, newTexts.get(ctx) + expr);
		}
		else if(ctx.getChildCount() == 3) {
			if(ctx.getChild(0).getText().equals("(")) { 		// '(' expr ')'
				expr += newTexts.get(ctx.expr(0));

			} else if(ctx.getChild(1).getText().equals("=")) { 	// IDENT '=' expr
				//IDENT가 전역변수인지 아닌지 확인 필요
				boolean isVar = !(symbolTable.isLocal(ctx.expr(0).getText()));

				//만약 전역변수라면
				if(isVar) {
                    idName = ctx.IDENT().getText();
                    if(symbolTable.getVarType(idName) == INT){
                        expr = "putfield " + "Test/" + idName +" " + "I" + "\n"
                                + "aload_0" + "\n";
                    }
                    else if(symbolTable.getVarType(idName) == FLOAT){
                        expr = "putfield " + "Test/" + idName +" " + "F" + "\n"
                                + "aload_0" + "\n";
                    }
				}
				else{
				    idName = ctx.IDENT().getText();
				    if(symbolTable.getVarType(idName) == INT) {
                        expr = newTexts.get(ctx.expr(0))
                                + "istore_" + symbolTable.getVarId(idName) + " \n";
                    }else if(symbolTable.getVarType(idName) == FLOAT){
                        expr = newTexts.get(ctx.expr(0))
                                + "fstore_" + symbolTable.getVarId(idName) + " \n";
                    }
				}

			} else { 											// binary operation
				expr += handleBinExpr(ctx, expr);

			}
		}
		// IDENT '(' args ')' |  IDENT '[' expr ']'
		else if(ctx.getChildCount() == 4) {
			if(ctx.args() != null){		// function calls
				expr = handleFunCall(ctx, expr);
			} else { // expr
				// Arrays: TODO
                idName = ctx.IDENT().getText();
				String expr1 = newTexts.get(ctx.expr(3));
				expr += "aload_"+symbolTable.getVarId(idName)+"\n"+"iconst_"+expr1+"\n";
			}
		}
		// IDENT '[' expr ']' '=' expr
		else { // Arrays: TODO			*/
			if(ctx.getChild(4).getText().equals("=")) {
				String expr1 = ctx.getChild(2).getText();
				String expr2 = ctx.getChild(5).getText();
                idName = ctx.IDENT().getText();
				expr += ("aload_"+symbolTable.getVarId(idName)+"\n"+"iconst_"+expr1+"\n" + "iconst_"+expr2+"\n" + "iastore\n");
			}
		}
		newTexts.put(ctx, expr);
	}


	private String handleUnaryExpr(MiniCParser.ExprContext ctx, String expr) {
		String l1 = symbolTable.newLabel();
		String l2 = symbolTable.newLabel();
		String lend = symbolTable.newLabel();

		expr += newTexts.get(ctx.expr(0));
		switch(ctx.getChild(0).getText()) {
		case "-":
			expr += "           ineg \n"; break;
		case "--":
			expr += "ldc 1" + "\n"
					+ "isub" + "\n";
			break;
		case "++":
			expr += "ldc 1" + "\n"
					+ "iadd" + "\n";
			break;
		case "!":
			expr += "ifeq " + l2 + "\n"
					+ l1 + ": " + "ldc 0" + "\n"
					+ "goto " + lend + "\n"
					+ l2 + ": " + "ldc 1" + "\n"
					+ lend + ": " + "\n";
			break;
		}
		return expr;
	}


	private String handleBinExpr(MiniCParser.ExprContext ctx, String expr) {
		String l2 = symbolTable.newLabel();
		String lend = symbolTable.newLabel();

		expr += newTexts.get(ctx.expr(0));
		expr += newTexts.get(ctx.expr(1));

		switch (ctx.getChild(1).getText()) {
		case "*":
			expr += "imul \n"; break;
		case "/":
			expr += "idiv \n"; break;
		case "%":
			expr += "irem \n"; break;
		case "+":		// expr(0) expr(1) iadd
			expr += "iadd \n"; break;
		case "-":
			expr += "isub \n"; break;

		case "==":
			expr += "isub " + "\n"
					+ "ifeq " + l2+ "\n"
					+ "ldc 0" + "\n"
					+ "goto " + lend + "\n"
					+ l2 + ": " + "ldc 1" + "\n"
					+ lend + ": " + "\n";
			break;
		case "!=":
			expr += "isub " + "\n"
					+ "ifne" + l2 + "\n"
					+ "ldc 0" + "\n"
					+ "goto " + lend + "\n"
					+ l2 + ": " + "ldc 1" + "\n"
					+ lend + ": " + "\n";
			break;
		case "<=":
			// x <= y일 경우
			expr += "isub " + "\n" // x - y의 값
					+ "ifle " + l2 + "\n" // 0보다 같거나 작으면 맞은 경우
					+ "ldc 0" + "\n" // 틀린 경우
					+ "goto " + lend + "\n"
					+ l2 + ": " + "ldc 1" + "\n"
					+ lend + ": ";
			break;
		case "<":
			// x < y인 경우
			expr += "isub " + "\n" // x- y 의 값
					+ "iflt " + l2 + "\n" // 0 보다 작은 경우, 맞은 경우
					+ "ldc 0" + "\n" // 틀린 경우
					+ "goto " + lend + "\n"
					+ l2 + ": " + "ldc 1" + "\n"
					+ lend + ": ";
			break;

		case ">=":
			// x >= y인 경우
			expr += "isub " + "\n" // x - y 의 값
					+ "ifge " + l2 + "\n" // 0보다 같거나 큰 경우, 맞은 경우
					+ "ldc 0" + "\n" // 틀린 경우
					+ "goto " + lend + "\n"
					+ l2 + ": " + "ldc 1" + "\n"
					+ lend + ": ";
			break;

		case ">":
			// x > y인 경우
			expr += "isub " + "\n" // x - y 의 값
					+ "ifgt " + l2 + "\n" // 0보다 큰 경우 , 맞은 경우
					+ "ldc 0" + "\n" // 틀린 경우
					+ "goto " + lend + "\n"
					+ l2 + ": " + "ldc 1" + "\n"
					+ lend + ": ";
			break;

		case "and":
			// x && y인 경우
			expr +=  "ifne "+ lend + "\n" // 0 과 같지 않으면 1이므로 반은 맞은 경우
			+ "pop" + "\n" + "ldc 0" + "\n" // 0과 같으면 아래 스택의 값은 확인 필요 없음, 뺴고 0 넣어줌
			+ lend + ": ";
			break;
		case "or":
			// x || y인 경우
			expr += "ifeq " + lend + "\n" // 0 과 같으면 한 번 더 확인 필요, lend로 감
			+ "pop" + "\n" + "ldc 1" + "\n" // 1이라면 더이상 확인할 필요 없으므로 pop하고 1 넣음
			+ lend + ": ";
			break;

		}
		return expr;
	}

	private String handleFunCall(MiniCParser.ExprContext ctx, String expr) {
		String fname = getFunName(ctx);

		if (fname.equals("_print")) {		// System.out.println
			expr = "getstatic java/lang/System/out Ljava/io/PrintStream; " + "\n"
					+ newTexts.get(ctx.args())
					+ "invokevirtual " + symbolTable.getFunSpecStr("_print") + "\n";
		} else {
			expr = newTexts.get(ctx.args())
					+ "invokestatic " + getCurrentClassName()+ "/" + symbolTable.getFunSpecStr(fname) + "\n";
		}

		return expr;

	}

	// args	: expr (',' expr)* | ;
	@Override
	public void exitArgs(MiniCParser.ArgsContext ctx) {
		String argsStr = "";
		for (int i=0; i < ctx.expr().size() ; i++) {
			argsStr += newTexts.get(ctx.expr(i));
		}
		newTexts.put(ctx, argsStr);
	}

}
