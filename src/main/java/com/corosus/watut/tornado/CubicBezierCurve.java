package com.corosus.watut.tornado;

import net.minecraft.util.math.vector.Vector3d;

public class CubicBezierCurve {
    private static final long serialVersionUID = -5219859720055898005L;
    private Vector3d[] P;

    /**
     * a contructor
     *
     * @param pointsVector 4 points that are required to build the bezier curve
     */
    public CubicBezierCurve(Vector3d[] pointsVector) {
        this.P = pointsVector;
    }


    /**
     * returns the point in 3d space that corresponds to the given value of t
     *
     * @param t curve's parameter that should be in the range [0, 1.0]
     * @return the point in 3d space that corresponds to the given value of t
     */
    public Vector3d getValue(double t) {
        if (t > 1.0 || t < 0.0) {
            throw new IllegalArgumentException("The value of t is out of range: " + t + " .");
        }
        double one_minus_t = 1 - t;
        Vector3d retValue = new Vector3d(0, 0, 0);
        Vector3d[] terms = new Vector3d[4];
        terms[0] = calcNewVector(one_minus_t * one_minus_t * one_minus_t, P[0]);
        terms[1] = calcNewVector(3 * one_minus_t * one_minus_t * t, P[1]);
        terms[2] = calcNewVector(3 * one_minus_t * t * t, P[2]);
        terms[3] = calcNewVector(t * t * t, P[3]);
        for (int i = 0; i < 4; i++) {
            retValue = retValue.add(terms[i]);
        }
        return retValue;
    }

    /**
     * calculates and returns a new vector that is base * scaler
     *
     * @param scaler
     * @param base
     * @return
     */
    private Vector3d calcNewVector(double scaler, Vector3d base) {
        //Vector3d retValue = new Vector3d(base);
        Vector3d retValue = new Vector3d(base.x, base.y, base.z);
        retValue = retValue.scale(scaler);
        return retValue;
    }

}