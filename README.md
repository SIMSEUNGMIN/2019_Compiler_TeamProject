# 2019_Compiler_TeamProject
2019 컴파일러 개론 팀프로젝트

test code 1

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

test code 2

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
