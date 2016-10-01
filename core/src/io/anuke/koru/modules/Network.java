package io.anuke.koru.modules;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;

import io.anuke.koru.Koru;
import io.anuke.koru.components.PositionComponent;
import io.anuke.koru.components.SyncComponent;
import io.anuke.koru.entities.KoruEntity;
import io.anuke.koru.network.IClient;
import io.anuke.koru.network.NetworkListener;
import io.anuke.koru.network.packets.ChunkPacket;
import io.anuke.koru.network.packets.ConnectPacket;
import io.anuke.koru.network.packets.DataPacket;
import io.anuke.koru.network.packets.EntityRemovePacket;
import io.anuke.koru.network.packets.PositionPacket;
import io.anuke.koru.network.packets.TileUpdatePacket;
import io.anuke.koru.network.packets.WorldUpdatePacket;
import io.anuke.koru.utils.Angles;
import io.anuke.koru.world.World;
import io.anuke.ucore.modules.Module;

public class Network extends Module<Koru>{
	public static final String ip = System.getProperty("user.name").equals("cobalt") ? "localhost" : "75.179.181.100";
	public static final int port = 7575;
	public static final int ping = 0;
	public static final int packetFrequency = 3;
	public boolean initialconnect = false;
	public boolean connecting;
	private boolean chunksAdded = false;
	private Array<KoruEntity> queue = new Array<KoruEntity>();
	private ObjectSet<Long> entitiesToRemove = new ObjectSet<Long>();
	public IClient client;
	
	public void connect(){
		try{
			connecting =  true;
			client.addListener(new Listen());
			client.connect(ip, port);
			Koru.log("Connecting to server..");
			
			ConnectPacket packet = new ConnectPacket();
			packet.name = "a player";
			client.sendTCP(packet);
			Koru.log("Sent packet.");
		}catch(Exception e){
			connecting = false;
			e.printStackTrace();
			Koru.log("Connection failed!");
		}
		connecting = false;
		initialconnect = true;
	}

	class Listen extends NetworkListener{
		@Override
		public void received(Object object){
			try{
				if(object instanceof DataPacket){
					DataPacket data = (DataPacket)object;

					t.engine.removeAllEntities();
					Koru.log("Recieved " + data.entities.size() + " entities.");
					for(Entity entity : data.entities){
						queue.add((KoruEntity)entity);
					}
					getModule(ClientData.class).player.resetID(data.playerid);
					queue.add(getModule(ClientData.class).player);
					Koru.log("Recieved data packet.");
				}else if(object instanceof WorldUpdatePacket){
					WorldUpdatePacket packet = (WorldUpdatePacket)object;
					for(Long key : packet.updates.keySet()){
						KoruEntity entity = t.engine.getEntity(key);
						if(entity == null) continue;
						entity.mapComponent(SyncComponent.class).type.read(packet.updates.get(key), entity);
					}
				}else if(object instanceof ChunkPacket){
					ChunkPacket packet = (ChunkPacket)object;
					getModule(World.class).loadChunks(packet);
					chunksAdded = true;
				}else if(object instanceof TileUpdatePacket){
					TileUpdatePacket packet = (TileUpdatePacket)object;
					if(getModule(World.class).inBounds(packet.x, packet.y)) getModule(World.class).setTile(packet.x, packet.y, packet.tile);
					chunksAdded = true;
				}else if(object instanceof EntityRemovePacket){
					EntityRemovePacket packet = (EntityRemovePacket)object;
					entitiesToRemove.add(packet.id);
				}else if(object instanceof KoruEntity){
					KoruEntity entity = (KoruEntity)object;
					queue.add(entity);
				}
			}catch(Exception e){
				e.printStackTrace();
				Koru.log("Packet recieve error!");
			}
		}
	}

	@Override
	public void update(){
		while(queue.size != 0){
			KoruEntity entity = queue.pop();
			if(entity == null) continue;
			if(entitiesToRemove.contains(entity.getID())){
				entitiesToRemove.remove(entity.getID());
				continue;
			}
			entity.addSelf();
		}

		for(Long id : entitiesToRemove){
			t.engine.removeEntity(id);
		}

		if(chunksAdded){
			getModule(Renderer.class).updateTiles();
			chunksAdded = false;
		}

		if(entitiesToRemove.size > 10) entitiesToRemove.clear();

		if(Gdx.graphics.getFrameId() % packetFrequency == 0) sendUpdate();
	}

	private void sendUpdate(){
		PositionPacket pos = new PositionPacket();
		pos.x = getModule(ClientData.class).player.mapComponent(PositionComponent.class).x;
		pos.y = getModule(ClientData.class).player.mapComponent(PositionComponent.class).y;
		pos.mouseangle = Angles.mouseAngle(getModule(Renderer.class).camera, getModule(ClientData.class).player.getX(), getModule(ClientData.class).player.getY());
		client.sendUDP(pos);
	}

	public boolean connected(){
		return client.isConnected();
	}

	public boolean initialconnect(){
		return initialconnect;
	}
}