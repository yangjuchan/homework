import java.io.*;
import java.util.*;

/* 원시 프로그램을 한줄을 받아서
 * 각각을 LABEL, OPCODE, OPERAND로 구분하고
 * BYTE 지시자를 사용시 X 인지 C 인지를 구분 하며
 * 4형식인지 아닌지를 구분 하며
 * 직접주소 지정 방식인지 간접 주소지정 방신인지를 구분 하며
 * 인덱스 레지스터를 사용하는지를 구분 하는 클래스 */
 
class Tokenizer{
        
        /* 각각의 모드 표시 */
        
        // OPCODE 가 BYTE 일때 OPERAND에 'X' 인지 'C' 인지 설정 
        static char BYTE_MODE = 'N';
        // OPCODE 가 4형식인지 : 'F' 아닌지 : 'T' 설정 
        static char TYPE_MODE = 'T';
        // 주소 지정 방식이 직접 인지 : 'I' 간접인지 : 'N' 일반형인지 : 'D' 설정
        static char ADDR_MODE = 'D';
        // 인덱스 레지스터를 사용 하는지 : 'Y' 안사용하는지 'N' 설정
        static char X_MODE = 'N';
        
        static char XE_MODE = 'N'; // SIC와 SIC/XE 버젼 구분
        
// --------------------------------------------------------------------------- //        
        
        /* 각 멤버 변수를 스태틱으로 선언한다 */
        static String LABEL = "";
        static String OPCODE = "";
        static String OPERAND = "";
        static int tokenNum = 0;
        
// --------------------------------------------------------------------------- //        
        
        /* 실제로 라인을 파싱 하는 메서드로서 스태틱을 선언한다. */
        
        static void parse(String str){
        	
        	StringTokenizer t = new StringTokenizer(str, " ");
        	tokenNum = t.countTokens();
        	String firstElement = t.nextToken();
        	
        	// 주석부분에 대한 처리
        	if (firstElement.equals(".")){
        		LABEL = ".";
        		return;
        	}
        	
        	// 정상적으로 LABEL, OPCODE, OPERAND 를 다 받았을떄 처리부분
        	if(tokenNum ==3){
        		LABEL = firstElement;
        		OPCODE = t.nextToken();
        		OPERAND = t.nextToken();
        	}
        	
        	// 2개 만 받았을경우 처리
        	else if(tokenNum == 2){
        		//if(OPTAB.getCode(firstElement) != null){ // 레이블이 없을경우
        			LABEL = "";
        			OPCODE = firstElement;
        			OPERAND = t.nextToken();
        		//}
        		/*else{ // 오퍼랜드가 없을경우
        			LABEL = firstElement;
        			OPCODE = t.nextToken();
        			OPERAND = "";
        		}*/
        	}
        	
        	// 1형식 명령과 같이 OPCODE 만 존재 하는 부분의 처리
        	else if(tokenNum == 1){
        		LABEL = "";
        		OPCODE = firstElement;
        		OPERAND = "";
        	}
        	
            // --------------------------------------------------------------- //
                    	
         	if(tokenNum >= 2){ // @토큰 2개 이상이라고 처리 안하면 에러발생@
        	    
        	    /* OPERAND 에서 작은 따옴표가 발견 되면
        	     * OPCODE 부분에 BYTE가 들어 옴을 알아 채고
        	     * 그 부분을 처리 */
        	
           	    // 작은 따옴표의 위치를 저장 하는 변수
           	    int byteModeCheck = OPERAND.indexOf("'");
        	
        	    // 작은 따옴표를 발견했을때 처리 즉, BYTE 지시자 일때 처리
        	    if (byteModeCheck >= 0){
        		    // 'X' 나 'C' 가 저장 되는 변수
        		    String byteMode = OPERAND.substring(0, byteModeCheck);
        		    // 실제 OPERAND 가 저장 되는 변수
        		    String byteOperand = OPERAND.substring(byteModeCheck+1,OPERAND.lastIndexOf("'"));
        		
        		    // 'X' 일때의 처리
        		    if(byteMode.equals("X")){
        			    BYTE_MODE = 'X';
        			    OPERAND = byteOperand;
        		    }
        		    // 'C' 일때의 처리
        		    else if(byteMode.equals("C")){
        			    BYTE_MODE = 'C';
        			    OPERAND = byteOperand;
           		    }
        	    }
        	    // BYTE 가 이닐때 처리
        	    else{
        		  	BYTE_MODE = 'N';
        		}
        		
        	// --------------------------------------------------------------- //	
        	
        	    /* 4형식일때 처리 부분 '+' 를 감지 하여 체크 한다. */
        	
        	    // OPCODE 첫 단어를 변수에 저장
        	    String typeMode = OPCODE.substring(0,1);
        	    // OPCODE 첫 단어가 + 이면 4형식 모드 설정 
        	    if (typeMode.equals("+")){
          		    TYPE_MODE = 'F';
          		    XE_MODE = 'Y';
        		    OPCODE = OPCODE.substring(1);
        	    }
        	    // 4형식이 아닐때 처리
        	    else{
        		    TYPE_MODE = 'T';
        	    }
        	    
        	// --------------------------------------------------------------- //    
        	
        	    /* 주소 지정 방식을 결정하여 설정 해준다(즉시,직접,디폴트). */
        	
        	    // OPERAND 첫번재 문자를 저장
        	    String addrMode = OPERAND.substring(0,1);
        	    // 즉시 주소 지정 방식 처리
        	    if (addrMode.equals("#")){
        		    ADDR_MODE = 'I';
        		    XE_MODE = 'Y';
        		    OPERAND = OPERAND.substring(1);
        	    }
        	    // 간접 주소 지정 방식 처리
        	    else if(addrMode.equals("@")){
        		    ADDR_MODE = 'N';
        		    XE_MODE = 'Y';
        		    OPERAND = OPERAND.substring(1);
        	    }
        	    // 둘다 아니면 
        	    else{
        		    ADDR_MODE = 'D';
        	    }
        	    
        	// --------------------------------------------------------------- //    
        	
        	    /* 인덱스 레지스터의 사용 유무 부분 처리
        	     * 원시코드에서 ',X' 부분은 꼭 붙여 써야만 처리 가능 */
        	
          	    // ',X' 부분의 인덱스 값을 저장 하는 변수
        	    int xModeCheck = OPERAND.indexOf(",X");
         	
        	    // ',X' 부분이 있으면, 즉 인덱스 레지스터를 사용 하면
        	    if (xModeCheck >= 0){
         		    X_MODE = 'Y';
        		    OPERAND = OPERAND.substring(0,xModeCheck);
        	    }
        	    // 인덱스 레지스터를 사용 안하면
        	    else{
          		    X_MODE = 'N';
        	    }
            }
        }
 }