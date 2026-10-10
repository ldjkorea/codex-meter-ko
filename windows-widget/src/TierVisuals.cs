using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.Runtime.InteropServices;

namespace CodexMeterWidget {
    // Same presentation palette as Android WidgetTierPalette. Never evaluates a tier.
    public static class TierVisuals {
        public static readonly int[] TopRgb={0x343e4b,0x503628,0x3e4c5c,0x554323,0x214652,0x174e3d,0x224868,0x483464,0x542d3c,0x5e4927};
        public static readonly int[] BottomRgb={0x111820,0x211711,0x171f29,0x251d10,0x102329,0x0b251e,0x102130,0x1d142c,0x28131d,0x251c0e};
        public static readonly int[] AccentRgb={0xa9b3bd,0xdfa778,0xd5e1eb,0xe8ce91,0xaee4ee,0x68e1b1,0x9bdcfa,0xc7a7f4,0xf1a0ac,0xffdfa1};
        public static bool Active(int tier){return tier>=0&&tier<10;}
        static Color Rgb(int rgb){return Color.FromArgb(unchecked((int)(0xff000000u|(uint)rgb)));}
        public static Color Top(int tier){return Active(tier)?Rgb(TopRgb[tier]):Color.FromArgb(22,25,29);}
        public static Color Bottom(int tier){return Active(tier)?Rgb(BottomRgb[tier]):Color.FromArgb(13,14,16);}
        public static Color Accent(int tier){return Active(tier)?Rgb(AccentRgb[tier]):Color.FromArgb(190,196,204);}
        public static int VisibleTier(Snapshot snapshot,TierState rank,bool failed){
            return snapshot!=null&&snapshot.Main!=null&&snapshot.Fresh(Clock.Now)&&!failed&&rank!=null&&Active(rank.Tier)?rank.Tier:-1;
        }
        static GraphicsPath Round(RectangleF r,float radius){
            var path=new GraphicsPath();float d=radius*2;
            path.AddArc(r.X,r.Y,d,d,180,90);path.AddArc(r.Right-d,r.Y,d,d,270,90);
            path.AddArc(r.Right-d,r.Bottom-d,d,d,0,90);path.AddArc(r.X,r.Bottom-d,d,d,90,90);path.CloseFigure();return path;
        }
        public static void Frame(Graphics g,Size size,int tier){
            g.Clear(Bottom(tier));var bounds=new RectangleF(2,2,size.Width-5,size.Height-5);
            using(var shape=Round(bounds,14))using(var background=new LinearGradientBrush(bounds,Top(tier),Bottom(tier),90f))
            using(var edge=new Pen(Color.FromArgb(155,Accent(tier)),1.3f)){g.FillPath(background,shape);g.DrawPath(edge,shape);}
            if(!Active(tier))return;
            if(tier>=3)using(var shape=Round(new RectangleF(6,6,size.Width-13,size.Height-13),11))
                using(var edge=new Pen(Color.FromArgb(48,Accent(tier))))g.DrawPath(edge,shape);
            using(var line=new Pen(Color.FromArgb(105,Accent(tier)),1)){
                g.DrawLine(line,18,43,size.Width-18,43);
                int strokes=1+tier/3;
                for(int i=0;i<strokes;i++){float y=size.Height-12-i*4;
                    g.DrawLine(line,12,y,25+i*5,y);g.DrawLine(line,size.Width-12,y,size.Width-25-i*5,y);}
                int gems=1+tier/2;float center=size.Width*.58f;
                for(int i=0;i<gems;i++){float x=center+(i-(gems-1)/2f)*12;
                    if(tier>=5)g.DrawPolygon(line,new[]{new PointF(x,15),new PointF(x+3,19),new PointF(x,23),new PointF(x-3,19)});
                    else g.DrawLine(line,x,17,x,22);
                }
            }
        }
        public static void CrestPlate(Graphics g,RectangleF rect,int tier){
            if(!Active(tier))return;var ring=new RectangleF(rect.X+4,rect.Y+4,rect.Width-8,rect.Height-8);
            using(var fill=new SolidBrush(Color.FromArgb(40,Accent(tier))))g.FillEllipse(fill,ring);
            using(var edge=new Pen(Color.FromArgb(tier>=7?155:80,Accent(tier)),tier>=7?1.4f:1))g.DrawEllipse(edge,ring);
            if(tier>=7){ring.Inflate(3,3);using(var edge=new Pen(Color.FromArgb(55,Accent(tier))))g.DrawEllipse(edge,ring);}
        }
        [DllImport("user32.dll")]static extern bool DestroyIcon(IntPtr handle);
        public static Icon CreateIcon(Image[] images,int tier){
            if(images==null||images.Length!=10)return null;
            Image image=images[Active(tier)?tier:0];if(image==null)return null;
            using(var bitmap=new Bitmap(32,32,PixelFormat.Format32bppArgb))using(var g=Graphics.FromImage(bitmap)){
                g.InterpolationMode=InterpolationMode.HighQualityBicubic;
                if(Active(tier))g.DrawImage(image,new Rectangle(0,0,32,32));
                else using(var attributes=new ImageAttributes()){
                    var matrix=new ColorMatrix();matrix.Matrix33=.5f;attributes.SetColorMatrix(matrix);
                    g.DrawImage(image,new Rectangle(0,0,32,32),0,0,image.Width,image.Height,GraphicsUnit.Pixel,attributes);
                }
                IntPtr handle=bitmap.GetHicon();try{using(var borrowed=Icon.FromHandle(handle))return (Icon)borrowed.Clone();}finally{DestroyIcon(handle);}
            }
        }
    }
}
