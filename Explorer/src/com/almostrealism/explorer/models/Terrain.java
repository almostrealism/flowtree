package com.almostrealism.explorer.models;

import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;

import com.almostrealism.raytracer.primitives.Mesh;
import com.almostrealism.texture.ImageLayers;
import com.almostrealism.visualize.primitives.RenderableMesh;

public class Terrain extends RenderableMesh {
	private ImageLayers textures;
	
	public Terrain(String shapeURL) {
		super(createMesh());
		
		textures = new ImageLayers();
		
		try {
			Map connect = new HashMap();
			connect.put("url", shapeURL);
			
			DataStore dataStore = DataStoreFinder.getDataStore(connect);
			String[] typeNames = dataStore.getTypeNames();
			String typeName = typeNames[0];
			
			System.out.println("Reading content " + typeName);
			
			FeatureSource featureSource = dataStore.getFeatureSource(typeName);
			FeatureCollection collection = featureSource.getFeatures();

			try (FeatureIterator iterator = collection.features()) {
				while (iterator.hasNext()) {
					Feature feature = iterator.next();
					System.out.println(feature);
//					Geometry sourceGeometry = feature.getDefaultGeometryProperty().getDescriptor().;
				}
			}
		} catch (Throwable e) { }
	}
	
	public ImageLayers getTexture() { return textures; }
	
	private static Mesh createMesh() {
		return null;
	}
}