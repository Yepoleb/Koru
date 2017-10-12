package io.anuke.koru.server.world;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.IntIntMap;

import io.anuke.koru.world.Generator;
import io.anuke.koru.world.Tile;
import io.anuke.koru.world.materials.Material;
import io.anuke.koru.world.materials.MaterialLayer;
import io.anuke.koru.world.materials.MaterialTypes;
import io.anuke.koru.world.materials.MaterialTypes.Grass;
import io.anuke.koru.world.materials.MaterialTypes.Wall;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.graphics.Atlas;
import io.anuke.ucore.noise.RidgedPerlin;
import io.anuke.ucore.noise.VoronoiNoise;
import io.anuke.ucore.util.GridMap;
import io.anuke.ucore.util.Tmp;

public class MapPreview extends ApplicationAdapter{
	String atlasDir = System.getProperty("user.home") + "/Projects/Koru/core/assets/sprites/sprites.atlas";
	Generator gen;
	GridMap<ChunkPix> blocks = new GridMap<ChunkPix>();
	IntIntMap colors = new IntIntMap();
	SpriteBatch batch;
	int viewrange = 4;
	float scl = 2f;
	float vx, vy;
	float speed = 32;
	int pixsize = 64;
	int percision = 1;
	boolean showDark = false;
	static double maxe;
	static double maxt;

	public void create(){
		batch = new SpriteBatch();
		gen = new OctaveTerrainGenerator();
		
		Gdx.graphics.setContinuousRendering(false);

		Atlas atlas = new Atlas(Gdx.files.absolute(atlasDir));

		Pixmap pixmap = atlas.getPixmapOf(atlas.findRegion("grass"));

		for(Material material : Material.getAll()){
			if(!material.isLayer(MaterialLayer.floor) && !(material instanceof Wall))
				continue;
			
			if(material instanceof Grass){
				Color color = Tmp.c1.set(MaterialTypes.grasscolor.r * material.foilageTint().x,
						MaterialTypes.grasscolor.g * material.foilageTint().y,
						MaterialTypes.grasscolor.b * material.foilageTint().z, 1f);
				
				colors.put(material.id(), Color.rgba8888(color));
			}else{
			
				AtlasRegion region = atlas.findRegion(material.name());
				colors.put(material.id(), pixmap.getPixel(region.getRegionX(), region.getRegionY()));
			}
		}

		for(int x = -viewrange; x <= viewrange; x++){
			for(int y = -viewrange; y <= viewrange; y++){
				gen(x, y);
			}
		}
	}

	public void render(){
		if(Gdx.input.isKeyPressed(Keys.ESCAPE))
			Gdx.app.exit();

		float speed = Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) ? this.speed * 5 : this.speed;

		if(Gdx.input.isKeyPressed(Keys.W))
			vy += speed;
		if(Gdx.input.isKeyPressed(Keys.A))
			vx -= speed;
		if(Gdx.input.isKeyPressed(Keys.S))
			vy -= speed;
		if(Gdx.input.isKeyPressed(Keys.D))
			vx += speed;

		Graphics.clear(Color.BLACK);
		batch.begin();

		int viewx = (int) (vx / pixsize);
		int viewy = (int) (vy / pixsize);
		for(int x = -viewrange + viewx; x <= viewrange + viewx; x++){
			for(int y = -viewrange + viewy; y <= viewrange + viewy; y++){
				ChunkPix pix = blocks.get(x, y);
				if(pix != null){
					batch.draw(pix.texture, x * pixsize * scl + Gdx.graphics.getWidth() / 2 - vx*scl,
							y * pixsize * scl + Gdx.graphics.getHeight() / 2 - vy*scl, pixsize * scl, pixsize * scl);
				}else{
					gen(x, y);
				}
			}
		}
		batch.end();
	}

	void gen(int x, int y){
		blocks.put(x, y, new ChunkPix(x, y));
	}
	

	VoronoiNoise noise = new VoronoiNoise(0,(short) 0){{
		this.setUseDistance(true);
	}};
	
	RidgedPerlin per = new RidgedPerlin(1, 1, 0.4f);
	
	Color temp = new Color();
	
	int getPix(int x, int y){
		Tile tile = gen.generate(x, y);
		float light = showDark ? tile.light() : 1f;
		
		if(tile.wall() instanceof Wall){
			temp.set(colors.get(tile.wallid, 0));
			temp.mul(light, light, light, 1f);
			return Color.rgba8888(temp);
		}
		
		if(!tile.isWallEmpty()){
			temp.set(colors.get(tile.topFloor().id(), 0)+1000);
			temp.mul(light, light, light, 1f);
			return Color.rgba8888(temp);
		}
		
		temp.set(colors.get(tile.topFloor().id(), 0));
		temp.mul(light, light, light, 1f);
		
		return Color.rgba8888(temp);
	}

	class ChunkPix{
		Pixmap pixmap;
		Texture texture;

		public ChunkPix(int x, int y) {
			pixmap = new Pixmap(pixsize/percision, pixsize/percision, Format.RGBA8888);
			
			for(int px = 0; px < pixmap.getWidth(); px++){
				for(int py = 0; py < pixmap.getHeight(); py++){

					
					pixmap.drawPixel(px, (pixmap.getHeight() - 1 - py), getPix(x * pixsize + px * percision, y * pixsize + py * percision));
				}
			}
			texture = new Texture(pixmap);
		}
	}

	public void resize(int width, int height){
		batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
	}
}
