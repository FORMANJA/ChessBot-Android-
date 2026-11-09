package com.example.chessbot;

import java.util.Locale;

/** Experimental move tracker. Requires starting position and exactly one move between captures.
 *  Not a full legal-move validator; refuses ambiguous observations.
 */
public final class MoveTracker {
    private final char[][] board=new char[8][8];
    private boolean whiteTurn=true;
    public MoveTracker(){reset();}
    public void reset(){
        String[] rows={"rnbqkbnr","pppppppp","........","........","........","........","PPPPPPPP","RNBQKBNR"};
        for(int r=0;r<8;r++)board[r]=rows[r].toCharArray();
        whiteTurn=true;
    }
    public String observe(double[] changes,double threshold){
        if(changes==null||changes.length!=64)return "64 karelik veri gerekli";
        int[] candidate=new int[4];int count=0;
        for(int i=0;i<64;i++)if(changes[i]>=threshold){
            if(count<4)candidate[count]=i;
            count++;
        }
        if(count!=2)return "Belirsiz değişim: "+count+" kare. Tam bir hamle için 2 kare bekleniyor.";
        int a=candidate[0],b=candidate[1];
        int from=-1,to=-1;
        if(isOwn(board[a/8][a%8])&&canMove(a,b)){from=a;to=b;}
        if(isOwn(board[b/8][b%8])&&canMove(b,a)){
            if(from!=-1)return "Belirsiz yön; işlem yapılmadı";
            from=b;to=a;
        }
        if(from<0)return "Taş veya hareket eşleşmedi; işlem yapılmadı";
        char piece=board[from/8][from%8];
        char dest=board[to/8][to%8];
        if(dest!='.' && Character.isUpperCase(dest)==Character.isUpperCase(piece))return "Dost taş engeli";
        if(Character.toLowerCase(piece)=='p'&&(to/8==0||to/8==7))return "Terfi henüz desteklenmiyor";
        board[to/8][to%8]=piece;board[from/8][from%8]='.';
        whiteTurn=!whiteTurn;
        return String.format(Locale.US,"Aday hamle: %c%d%c%d (uygulandı; tam yasallık kontrolü yok)",
            'a'+from%8,8-from/8,'a'+to%8,8-to/8);
    }
    private boolean isOwn(char p){return p!='.' && Character.isUpperCase(p)==whiteTurn;}
    private boolean canMove(int a,int b){
        char p=board[a/8][a%8];if(!isOwn(p))return false;
        int r=a/8,c=a%8,R=b/8,C=b%8,dr=R-r,dc=C-c;
        char dest=board[R][C];
        if(dest!='.'&&Character.isUpperCase(dest)==whiteTurn)return false;
        switch(Character.toLowerCase(p)){
            case 'n':return (Math.abs(dr)==2&&Math.abs(dc)==1)||(Math.abs(dr)==1&&Math.abs(dc)==2);
            case 'k':return Math.max(Math.abs(dr),Math.abs(dc))==1;
            case 'p':{
                int dir=whiteTurn?-1:1,start=whiteTurn?6:1;
                if(dc==0&&dest=='.'&&dr==dir)return true;
                if(dc==0&&dest=='.'&&dr==2*dir&&r==start&&board[r+dir][c]=='.')return true;
                return Math.abs(dc)==1&&dr==dir&&dest!='.';
            }
            case 'r':return (dr==0||dc==0)&&clear(r,c,R,C);
            case 'b':return Math.abs(dr)==Math.abs(dc)&&clear(r,c,R,C);
            case 'q':return (dr==0||dc==0||Math.abs(dr)==Math.abs(dc))&&clear(r,c,R,C);
            default:return false;
        }
    }
    private boolean clear(int r,int c,int R,int C){
        int sr=Integer.signum(R-r),sc=Integer.signum(C-c);
        if(sr==0&&sc==0)return false;
        for(int y=r+sr,x=c+sc;y!=R||x!=C;y+=sr,x+=sc)if(board[y][x]!='.')return false;
        return true;
    }
}
