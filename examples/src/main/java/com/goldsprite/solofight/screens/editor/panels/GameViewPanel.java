package com.goldsprite.solofight.screens.editor.panels;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.goldsprite.gdengine.ecs.component.SpriteComponent;
import com.goldsprite.gdengine.ecs.component.TransformComponent;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.solofight.screens.editor.EditorContext;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import java.util.List;

public class GameViewPanel extends BaseEditorPanel {
	private GameViewActor viewActor;

	public GameViewPanel(Skin skin, EditorContext context) {
		super("Game", skin, context);
	}

	@Override
	protected void initContent() {
		viewActor = new GameViewActor(context);
		getContent().add(viewActor).grow();
	}
	
	private class GameViewActor extends Widget {
		private final EditorContext context;
		private final NeonBatch batch;
		
		public GameViewActor(EditorContext context) {
			this.context = context;
			this.batch = new NeonBatch();
		}
		
		@Override
		public void layout() {
			// Ensure GameWorld camera matches the view panel size
			if (com.goldsprite.gdengine.ecs.GameWorld.worldCamera != null) {
				com.goldsprite.gdengine.ecs.GameWorld.worldCamera.viewportWidth = getWidth();
				com.goldsprite.gdengine.ecs.GameWorld.worldCamera.viewportHeight = getHeight();
				com.goldsprite.gdengine.ecs.GameWorld.worldCamera.update();
			}
		}

		@Override
		public void draw(Batch batch, float parentAlpha) {
			validate();

			Rectangle scissors = new Rectangle();
			Rectangle clipBounds = new Rectangle(getX(), getY(), getWidth(), getHeight());
			Vector2 screenPos = localToStageCoordinates(new Vector2(0, 0));
			clipBounds.x = screenPos.x;
			clipBounds.y = screenPos.y;

			ScissorStack.calculateScissors(getStage().getCamera(), batch.getTransformMatrix(), clipBounds, scissors);
			
			if (ScissorStack.pushScissors(scissors)) {
				batch.end();
				
				// Draw Game World (Entities) using global camera or separate game camera
				// For simplicity, we use the same camera as GameWorld usually does, but we need to manage it.
				// Here we just use a default projection or the one from context if available.
				// Assuming GameWorld.worldCamera is available and correct.
				
				if (com.goldsprite.gdengine.ecs.GameWorld.worldCamera != null) {
					this.batch.setProjectionMatrix(com.goldsprite.gdengine.ecs.GameWorld.worldCamera.combined);
					this.batch.begin();
					drawEntities(context.gameWorld.getRootEntities());
					this.batch.end();
				}

				batch.begin();
				ScissorStack.popScissors();
			}
		}
		
		private void drawEntities(List<GObject> entities) {
			for (GObject obj : entities) {
				drawEntity(obj);
				if (!obj.getChildren().isEmpty()) {
					drawEntities(obj.getChildren());
				}
			}
		}

		private void drawEntity(GObject entity) {
			SpriteComponent sprite = entity.getComponent(SpriteComponent.class);
			TransformComponent transform = entity.transform;

			if (sprite != null && sprite.isEnable() && sprite.region != null) {
				TextureRegion region = sprite.region;
				float x = transform.position.x + sprite.offsetX;
				float y = transform.position.y + sprite.offsetY;
				float w = sprite.width;
				float h = sprite.height;
				float rotation = transform.rotation;
				float scaleX = transform.scale * (sprite.flipX ? -1 : 1);
				float scaleY = transform.scale * (sprite.flipY ? -1 : 1);
				
				float drawX = x - w / 2f;
				float drawY = y - h / 2f;
				float originX = w / 2f;
				float originY = h / 2f;
				
				Color oldColor = batch.getColor();
				batch.setColor(sprite.color);
				batch.draw(region, drawX, drawY, originX, originY, w, h, scaleX, scaleY, rotation);
				batch.setColor(oldColor);
			}
		}
	}
}
