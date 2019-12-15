package listener.main;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import generated.MiniCParser;
import generated.MiniCParser.Fun_declContext;
import generated.MiniCParser.Local_declContext;
import generated.MiniCParser.ParamsContext;
import generated.MiniCParser.Type_specContext;
import generated.MiniCParser.Var_declContext;
import listener.main.SymbolTable.Type;
import static listener.main.BytecodeGenListenerHelper.*;


public class SymbolTable {

	//함수 또는 변수 타입
	enum Type {
		INT, INTARRAY, VOID, ERROR, FLOAT, FLOATARRAY
	}

	//변수
	static public class VarInfo {
		Type type; // 변수 타입
		int id; // 변수 타입에 해당하는 id
		int initVal; // 변수 초기화 값

		// 생성자
		public VarInfo(Type type, int id, int initVal) {
			this.type = type;
			this.id = id;
			this.initVal = initVal;
		}
		public VarInfo(Type type,  int id) {
			this.type = type;
			this.id = id;
			this.initVal = 0;
		}
	}

	//함수
	static public class FInfo {
		public String sigStr;
	}

	private Map<String, VarInfo> _lsymtable = new HashMap<>();	// local v.
	private Map<String, VarInfo> _gsymtable = new HashMap<>();	// global v.
	private Map<String, FInfo> _fsymtable = new HashMap<>();	// function


	private int _globalVarID = 0;
	private int _localVarID = 1;
	private int _labelID = 0;
	private int _tempVarID = 0;

	SymbolTable(){
		initFunDecl();
		initFunTable();
	}

	//처음에 symbolTable 생성시 초기화
	void initFunDecl(){		// at each func decl
		this._lsymtable.clear();
		_localVarID = 1;
		_labelID = 0;
		_tempVarID = 32;
	}

	void putLocalVar(String varname, Type type){
		//들어온 정보를 가지고 지역변수 테이블에 지역변수를 저장
		this._lsymtable.put(varname, new VarInfo(type, this._localVarID++));
	}

	void putGlobalVar(String varname, Type type){
		//들어온 정보를 가지고 전역변수 테이블에 전역변수를 저장
		this._gsymtable.put(varname, new VarInfo(type, this._globalVarID++));
	}

	void putLocalVarWithInitVal(String varname, Type type, int initVar){
		//들어온 정보를 가지고 지역변수 테이블에 지역변수를 저장 (초기화 포함)
		this._lsymtable.put(varname, new VarInfo(type, this._localVarID++, initVar));
	}

	void putGlobalVarWithInitVal(String varname, Type type, int initVar){
		//들어온 정보를 가지고 전역변수 테이블에 전역변수를 저장 (초기화 포함)
		this._gsymtable.put(varname, new VarInfo(type, this._globalVarID++, initVar));

//		for(String key : this._gsymtable.keySet()) {
//			System.out.println("key : " + key + ", value : " + this._gsymtable.get(key).initVal);
//		}
	}

	//여러 개의 변수를 정리하여 지역변수 테이블에 넣어줌
	void putParams(MiniCParser.ParamsContext params) {
		//변수 개수만큼 반복
		for(int i = 0; i < params.param().size(); i++) {
			//파라미터 변수 타입 (0번)
			String varTypeString = params.param().get(i).children.get(0).getText();
			Type type;

			//어느 변수 타입인지 확인
			if(varTypeString.equals("int"))
				type = Type.INT;
			else if(varTypeString.equals("int[]"))
				type = Type.INTARRAY;
			else if(varTypeString.equals("void"))
				type = Type.VOID;
			else if(varTypeString.equals("float"))
				type = Type.FLOAT;
			else if(varTypeString.equals("float[]"))
				type = Type.FLOAT;
			else
				type = Type.ERROR;

			//파라미터 변수 이름(1번)
			String varname = params.param().get(i).children.get(1).getText();

			//파라미터 변수 타입과 이름을 넣어줌
			this._lsymtable.put(varname, new VarInfo(type, this._localVarID++));
		}
	}

	private void initFunTable() {
		FInfo printlninfo = new FInfo();
		printlninfo.sigStr = "java/io/PrintStream/println(I)V";

		FInfo maininfo = new FInfo();
		maininfo.sigStr = "main([Ljava/lang/String;)V";
		_fsymtable.put("_print", printlninfo);
		_fsymtable.put("main", maininfo);
	}

	//(내부에서 함수를 호출할 때, 함수 정의 X)
	public String getFunSpecStr(String fname) {
		String funStr = "";

		//들어오는 인자값과 일치하는 함수명을 찾아 함수 형태 출력
		for(String key : this._fsymtable.keySet()) {
			if(fname.contains(key)) {
				funStr = this._fsymtable.get(key).sigStr;
			}
		}

		return funStr;
	}

	// (내부에서 함수를 호출할 때, 함수 정의 X)
	public String getFunSpecStr(Fun_declContext ctx) {
		System.out.println("getfunspecStr ctx : " + ctx);
		String funName = ctx.IDENT().getText();
		String funStr = "";

		//함수명과 일치하는 key를 찾아 함수 형태 출력
		for(String key : this._fsymtable.keySet()) {
			if(funName.contains(key)) {
				funStr = this._fsymtable.get(key).sigStr;
			}
		}

		return funStr;
	}

	// 함수 이름과 인자들을 원하는 형태로 만들어 집어넣음
	public String putFunSpecStr(Fun_declContext ctx) {
		String fname = getFunName(ctx);
		String argtype = ctx.params().getText();

		//argtype에서 인자들이 어떤 변수타입을 가지는지 확인하고 형태 변환
		String[] args = argtype.split(",");
		argtype = "";

		for(int i = 0; i < args.length; i++) {
			if(args[i].contains("int")) {
				argtype += "I";
			}
			else if(args[i].contains("float")){
				argtype += "F";
			}
			//			else if(args[i].contains("void")) {
			//				argtype += "V";
			//			}
			else; // 그 외
		}

		//반환형
		String rtype = ctx.type_spec().getText();
		if(rtype.equals("int")) {
			rtype = "I";
		}
		else if(rtype.equals("void"))
			rtype = "V";
		else if(rtype.equals("float"))
			rtype = "F";
		else; // 그 외

		//함수 형태
		String res = "";
		res =  fname + "(" + argtype + ")" + rtype;

		FInfo finfo = new FInfo();
		finfo.sigStr = res;
		_fsymtable.put(fname, finfo);

		return res;
	}

	boolean isLocal(String name) {
		VarInfo lname = (VarInfo) _lsymtable.get(name);

		if(lname != null)
		return true;

		else return false;
	}

	// 변수 id를 찾음
	String getVarId(String name){
		String sname = "";

		VarInfo lvar = (VarInfo) _lsymtable.get(name); // 지역 변수 테이블에서 찾음
		VarInfo gvar = (VarInfo) _gsymtable.get(name); // 전역 변수 테이블에서 찾음

		if(lvar != null) { // 만약 있다면 반환함
			sname += this._lsymtable.get(name).id;
			return sname;
		}
		else if(gvar != null) { // 만약 있다면 반환
			sname += this._gsymtable.get(name).id;
		}

		return sname;
	}

	Type getVarType(String name){
		VarInfo lvar = (VarInfo) _lsymtable.get(name); // 지역 변수 테이블에서 찾음
		if (lvar != null) { // 만약 있다면 반환함
			return lvar.type;
		}

		VarInfo gvar = (VarInfo) _gsymtable.get(name); // 전역 변수 테이블에서 찾음
		if (gvar != null) { // 만약 있다면 반환
			return gvar.type;
		}

		return Type.ERROR;
	}

	String newLabel() {
		return "label" + _labelID++;
	}

	String newTempVar() {
		String id = "";
		return id + _tempVarID--;
	}

	// global
	public String getVarId(Var_declContext ctx) {
		String sname = "";
		sname += getVarId(ctx.IDENT().getText());
		return sname;
	}

	// local
	public String getVarId(Local_declContext ctx) {
		String sname = "";
		sname += getVarId(ctx.IDENT().getText());
		return sname;
	}

}
