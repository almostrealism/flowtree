/*
 * Copyright 2016 Michael Murray
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.almostrealism.raytracer.graphics;

import java.awt.Graphics;

import org.almostrealism.space.TransformMatrix;
import org.almostrealism.space.Vector;

import com.almostrealism.raytracer.camera.PinholeCamera;
import com.almostrealism.raytracer.engine.ParticleGroup;

/**
 * @author Mike Murray
 */
public class ParticleGroupRenderer {
    public static void draw(ParticleGroup p, PinholeCamera c, Graphics g, double ox, double oy, double scale, double minSize, double maxSize, double far) {
        double v[][] = p.getParticleVertices();
        
        TransformMatrix m = c.getRotationMatrix();
        
        i: for (int i = 0; i < v.length; i++) {
            Vector l = new Vector(v[i][0], v[i][1], v[i][2]);
            m.transform(l, TransformMatrix.TRANSFORM_AS_LOCATION);
            
            if (l.getZ() < 0.0) continue i;
            
            double r = minSize + (maxSize - minSize) * (l.getZ() / far);
            
            double x = (l.getX() - r) * scale;
            double y = (l.getY() - r) * scale;
            
            g.fillOval((int)(ox + x), (int)(oy - y), (int)(2 * r * scale), (int)(2 * r * scale));
        }
    }
}
