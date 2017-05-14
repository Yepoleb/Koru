package io.anuke.koru.systems;

import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;

import io.anuke.koru.Koru;
import io.anuke.koru.components.ColliderComponent;
import io.anuke.koru.entities.KoruEntity;
import io.anuke.koru.modules.World;
import io.anuke.koru.world.Tile;
import io.anuke.ucore.core.Draw;

public class CollisionDebugSystem extends KoruSystem{

	public CollisionDebugSystem() {
		super(Family.all(ColliderComponent.class).get(), 0);
	}

	@Override
	public void update(float deltaTime){
		if(!Draw.batch().isDrawing())
			return;

		super.update(deltaTime);

		World world = World.instance();
		int tilex = World.tile(Koru.control.player.getX());
		int tiley = World.tile(Koru.control.player.getY());
		int range = 10;
		for(int rx = -range; rx <= range; rx++){
			for(int ry = -range; ry <= range; ry++){
				int worldx = tilex + rx, worldy = tiley + ry;
				if(!world.inClientBounds(worldx, worldy))
					continue;
				Tile tile = world.getTile(worldx, worldy);
				if(!tile.solid())
					continue;

				Rectangle out = tile.solidMaterial().getType().getHitbox(worldx, worldy, Rectangle.tmp2);
				Draw.color(Color.PURPLE);
				Draw.linerect(out.x, out.y, out.width, out.height);
				Draw.color(Color.WHITE);
			}
		}
	}

	@Override
	void processEntity(KoruEntity entity, float delta){
		ColliderComponent col = entity.collider();
		Rectangle bounds = col.getBounds(entity);

		Draw.color(Color.YELLOW);
		Draw.linerect(bounds.x, bounds.y, bounds.width, bounds.height);
		Draw.color();
	}
}
