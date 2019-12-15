# 2019_Compiler_TeamProject
2019 컴파일러 개론 팀프로젝트

test code 1 (복합)

int count;
float h = 1.0f;
float hiif[5];

float add(int a, float b){
	hiif[a] = b;
	return hiif[a];
}

void main(){
	int i = 5;
	float j = 1.2f;
	count = 1;
	
	while(count < i){
		if(count % 2 == 0){
			_print(add(count, h));
		}
		else{
         _print(add(count, j));
         ++j;
      	}
		--h;
		count = count + 1;
	}
}

test code 2 (단순)

void main(){
	float a = 2.3f;
	float b = 6.3f;
	
	if(a > b){
		_print(a);
	}
	else{
		_print(b);
	}
}

test code 3 (형변환)

int count = 0;
float h = 1.0f;
int hiif[5];

int add(float a, int b){
   hiif[0] = 3;
   return hiif[0];
}

void main(){
   int j[5];
   int c = 5;
   float a = 0.0f;
   
   j[1] = 1;
   j[2] = 2;
   
   j[1] = j[2];
   hiif[0] = j[1];
   a = 1.1f + 2.2f;
   
   a = c + 1.0f; 
   _print(hiif[0]); 
}
