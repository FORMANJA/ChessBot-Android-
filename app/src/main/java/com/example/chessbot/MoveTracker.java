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
    /** Rebuild from the standard opening using a verified chronological UCI move list. */
    public String syncHistory(String history) {
        reset();
        String trimmed=history==null?"":history.trim();
        if(trimmed.isEmpty())return "Başlangıç konumu yüklendi. Sıra: beyaz.";
        String[] moves=trimmed.split("[,;\\s]+");
        for(int i=0;i<moves.length;i++) {
            String m=moves[i].toLowerCase(Locale.US);
            if(!m.matches("[a-h][1-8][a-h][1-8]")) {
                reset();return "Geçersiz hamle ("+(i+1)+"): "+m+". Takip sıfırlandı.";
            }
            int from=(8-(m.charAt(1)-'0'))*8+(m.charAt(0)-'a');
            int to=(8-(m.charAt(3)-'0'))*8+(m.charAt(2)-'a');
            if(!canMove(from,to)) {
                reset();return "Konuma uymayan hamle ("+(i+1)+"): "+m+". Takip sıfırlandı.";
            }
            char piece=board[from/8][from%8];
            if(Character.toLowerCase(piece)=='p'&&(to/8==0||to/8==7)){
                reset();return "Terfi desteklenmiyor. Takip sıfırlandı.";
            }
            board[to/8][to%8]=piece;
            board[from/8][from%8]='.';
            whiteTurn=!whiteTurn;
        }
        return moves.length+" hamle yüklendi. Sıra: "+(whiteTurn?"beyaz":"siyah")+".";
    }

    /** Evaluate one or two sequential moves from the four strongest changed squares.
     * Refuses to mutate state unless exactly one candidate sequence is found.
     * Experimental: captures, promotions, castling and check legality not fully supported.
     */
    public String observeSequence(double[] scores) {
        if(scores==null||scores.length!=64)return "64 karelik veri gerekli";
        Integer[] order=new Integer[64];
        for(int i=0;i<64;i++)order[i]=i;
        java.util.Arrays.sort(order,(a,b)->Double.compare(scores[b],scores[a]));
        if(scores[order[1]]<18)return "Yeterli kare değişimi yok.";
        int[] top=new int[]{order[0],order[1],order[2],order[3]};
        java.util.LinkedHashMap<String,int[]> sequences=new java.util.LinkedHashMap<>();
        // First try a pair of consecutive moves (white then black, or vice versa).
        if(scores[top[3]]>=18) {
            for(int i=0;i<4;i++)for(int j=0;j<4;j++) {
                if(i==j)continue;
                int from=top[i],to=top[j];
                if(!canMove(from,to))continue;
                char p=board[from/8][from%8],dest=board[to/8][to%8];
                if(dest!='.' || (Character.toLowerCase(p)=='p'&&(to/8==0||to/8==7)))continue;
                board[to/8][to%8]=p;board[from/8][from%8]='.';whiteTurn=!whiteTurn;
                for(int k=0;k<4;k++)for(int l=0;l<4;l++){
                    if(k==l||k==i||k==j||l==i||l==j)continue;
                    int f2=top[k],t2=top[l];
                    if(canMove(f2,t2)&&board[t2/8][t2%8]=='.' &&
                       !(Character.toLowerCase(board[f2/8][f2%8])=='p'&&(t2/8==0||t2/8==7))) {
                        String name=notation(from,to)+" "+notation(f2,t2);
                        sequences.put(name,new int[]{from,to,f2,t2});
                    }
                }
                whiteTurn=!whiteTurn;board[from/8][from%8]=p;board[to/8][to%8]=dest;
            }
        }
        if(sequences.size()==1) {
            int[] m=sequences.values().iterator().next();
            apply(m[0],m[1]);apply(m[2],m[3]);
            return "İki hamle adayı: "+sequences.keySet().iterator().next()+" (deneysel)";
        }
        if(sequences.size()>1)return "Birden fazla iki-hamle olasılığı var ("+sequences.size()+"). Takip değiştirilmedi.";
        // Only accept a single move when there is a strong gap after the second square.
        if(scores[order[1]]<scores[order[2]]*1.65+8)
            return "Bir veya iki hamle belirsiz; takip değiştirilmedi.";
        int a=order[0],b=order[1];
        boolean ab=canMove(a,b)&&board[b/8][b%8]=='.';
        boolean ba=canMove(b,a)&&board[a/8][a%8]=='.';
        if(ab==ba)return "Tek hamle yönü belirlenemedi; takip değiştirilmedi.";
        int from=ab?a:b,to=ab?b:a;
        if(Character.toLowerCase(board[from/8][from%8])=='p'&&(to/8==0||to/8==7))
            return "Terfi desteklenmiyor.";
        apply(from,to);
        return "Tek hamle adayı: "+notation(from,to)+" (deneysel)";
    }
    private void apply(int from,int to) {
        board[to/8][to%8]=board[from/8][from%8];
        board[from/8][from%8]='.';
        whiteTurn=!whiteTurn;
    }
    private String notation(int from,int to) {
        return ""+(char)('a'+from%8)+(8-from/8)+(char)('a'+to%8)+(8-to/8);
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
