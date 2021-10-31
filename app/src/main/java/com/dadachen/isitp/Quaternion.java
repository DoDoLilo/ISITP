package com.dadachen.isitp;

public class Quaternion {
    private float x0;
    private float x1;
    private float x2;
    private float x3;

    // 四元数构造函数
    public Quaternion(float x0, float x1, float x2, float x3) {
        this.x0 = x0;
        this.x1 = x1;
        this.x2 = x2;
        this.x3 = x3;
    }

    // 转化
    public String toString() {
        return x0 + " + " + x1 + "i + " + x2 + "j + " + x3 + "k";
    }

    //to FloatArray
    public float[] toFloatArray(){
        return new float[]{x0,x1,x2,x3};
    }

    // 模
//    public double norm() {
//        return Math.sqrt(x0*x0 + x1*x1 +x2*x2 + x3*x3);
//    }

    // 共轭
    public Quaternion conjugate() {
        return new Quaternion(x0, -x1, -x2, -x3);
    }

    // 加法
    public Quaternion plus(Quaternion b) {
        Quaternion a = this;
        return new Quaternion(a.x0+b.x0, a.x1+b.x1, a.x2+b.x2, a.x3+b.x3);
    }


    // 乘法
    public Quaternion times(Quaternion b) {
        Quaternion a = this;
        float y0 = a.x0*b.x0 - a.x1*b.x1 - a.x2*b.x2 - a.x3*b.x3;
        float y1 = a.x0*b.x1 + a.x1*b.x0 + a.x2*b.x3 - a.x3*b.x2;
        float y2 = a.x0*b.x2 - a.x1*b.x3 + a.x2*b.x0 + a.x3*b.x1;
        float y3 = a.x0*b.x3 + a.x1*b.x2 - a.x2*b.x1 + a.x3*b.x0;
        return new Quaternion(y0, y1, y2, y3);
    }

    // 逆
    public Quaternion inverse() {
        float d = x0*x0 + x1*x1 + x2*x2 + x3*x3;
        return new Quaternion(x0/d, -x1/d, -x2/d, -x3/d);
    }


    // 除法
    // 转化为乘法问题 a * b^-1 (b^-1 a)
    public Quaternion divides(Quaternion b) {
        Quaternion a = this;
        return a.times(b.inverse());
    }

    public static void main(String[] args) {
        Quaternion accQuaternion=new Quaternion(0f, 0.78f, -0.21f, 13.49f);
        Quaternion gyroQuaternion=new Quaternion(0f, 0.42f, 0.21f, -0.27f);
        Quaternion rotQuaternion=new Quaternion(0.01f, 0.46f, 0.88f, 0.05f);
        float[] gyroChanged=rotQuaternion.times(gyroQuaternion).times(rotQuaternion.conjugate()).toFloatArray();
        float[] accChanged=rotQuaternion.times(accQuaternion).times(rotQuaternion.conjugate()).toFloatArray();
        System.out.println(gyroChanged);
        System.out.println(accChanged);
    }
}
