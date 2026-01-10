package com.goldsprite.solofight.screens.editor.panels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.solofight.screens.editor.EditorContext;
import com.goldsprite.solofight.screens.editor.adapter.GObjectAdapter;
import com.goldsprite.solofight.screens.tests.iconeditor.system.GizmoSystem;

public class SceneViewPanel extends BaseEditorPanel {
    private SceneViewActor viewActor;

    public SceneViewPanel(Skin skin, EditorContext context) {
        super("Scene", skin, context);
    }

    @Override
    protected void initContent() {
        viewActor = new SceneViewActor(context);
        getContent().add(viewActor).grow();
    }

    private class SceneViewActor extends Widget {
        private final EditorContext context;
        private final OrthographicCamera camera;
        private final NeonBatch batch;
        private final ShapeRenderer shapeRenderer;

        private boolean isDraggingCamera = false;
        private float lastX, lastY;
        
        // Gizmo Dragging State
        private boolean isDraggingGizmo = false;
        private int dragPointer = -1;

        public SceneViewActor(EditorContext context) {
            this.context = context;
            this.camera = new OrthographicCamera();
            this.batch = new NeonBatch();
            this.shapeRenderer = new ShapeRenderer();

            addListener(new InputListener() {
                @Override
                public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
                    camera.zoom += amountY * 0.1f * camera.zoom;
                    camera.zoom = MathUtils.clamp(camera.zoom, 0.01f, 100f);
                    camera.update();
                    return true;
                }

                @Override
                public boolean keyDown(InputEvent event, int keycode) {
                    if (keycode == Input.Keys.W) context.gizmoSystem.mode = GizmoSystem.Mode.MOVE;
                    if (keycode == Input.Keys.E) context.gizmoSystem.mode = GizmoSystem.Mode.ROTATE;
                    if (keycode == Input.Keys.R) context.gizmoSystem.mode = GizmoSystem.Mode.SCALE;
                    return true;
                }

                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    getStage().setKeyboardFocus(SceneViewActor.this);
                    
                    Vector2 worldPos = screenToWorld(x, y);

                    if (button == Input.Buttons.RIGHT || (button == Input.Buttons.MIDDLE)) {
                        isDraggingCamera = true;
                        lastX = x;
                        lastY = y;
                        return true;
                    }

                    if (button == Input.Buttons.LEFT) {
                        // TODO: Integrate full GizmoSystem input logic here
                        // For now, just simple selection
                        handleSelection(worldPos.x, worldPos.y);
                        return true;
                    }
                    return false;
                }

                @Override
                public void touchDragged(InputEvent event, float x, float y, int pointer) {
                    if (isDraggingCamera) {
                        float dx = x - lastX;
                        float dy = y - lastY;
                        camera.translate(-dx * camera.zoom, -dy * camera.zoom);
                        camera.update();
                        lastX = x;
                        lastY = y;
                    }
                }

                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                    isDraggingCamera = false;
                }
            });
        }

        private Vector2 screenToWorld(float x, float y) {
            float wx = (x - getWidth() / 2) * camera.zoom + camera.position.x;
            float wy = (y - getHeight() / 2) * camera.zoom + camera.position.y;
            return new Vector2(wx, wy);
        }

        private void handleSelection(float wx, float wy) {
            for (GObject obj : context.gameWorld.getRootEntities()) {
                if (checkHitRecursive(obj, wx, wy)) {
                    return;
                }
            }
            context.setSelection(null);
        }

        private boolean checkHitRecursive(GObject obj, float wx, float wy) {
            for (int i = obj.getChildren().size() - 1; i >= 0; i--) {
                if (checkHitRecursive(obj.getChildren().get(i), wx, wy)) return true;
            }

            GObjectAdapter adapter = new GObjectAdapter(obj);
            if (adapter.hitTest(wx, wy)) {
                context.setSelection(obj);
                return true;
            }
            return false;
        }

        @Override
        public void draw(com.badlogic.gdx.graphics.g2d.Batch batch, float parentAlpha) {
            validate();

            Rectangle scissors = new Rectangle();
            Rectangle clipBounds = new Rectangle(getX(), getY(), getWidth(), getHeight());
            
            // Transform local clip bounds to screen coordinates
            // Note: This simplified calculation assumes no rotation in UI hierarchy
            Vector2 screenPos = localToStageCoordinates(new Vector2(0, 0));
            clipBounds.x = screenPos.x;
            clipBounds.y = screenPos.y;

            ScissorStack.calculateScissors(getStage().getCamera(), batch.getTransformMatrix(), clipBounds, scissors);
            
            if (ScissorStack.pushScissors(scissors)) {
                batch.end();

                // Draw Background
                Gdx.gl.glEnable(GL20.GL_BLEND);
                shapeRenderer.setProjectionMatrix(camera.combined);
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(0.15f, 0.15f, 0.15f, 1);
                shapeRenderer.rect(camera.position.x - camera.viewportWidth/2 * camera.zoom, 
                                 camera.position.y - camera.viewportHeight/2 * camera.zoom, 
                                 camera.viewportWidth * camera.zoom, 
                                 camera.viewportHeight * camera.zoom);
                shapeRenderer.end();

                // Draw Grid
                drawGrid();

                // Draw Game World (Entities)
                // TODO: Implement entity rendering
                // For now, draw debug circles for entities
                shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
                drawEntitiesDebug(context.gameWorld.getRootEntities());
                shapeRenderer.end();

                // Draw Gizmos
                this.batch.setProjectionMatrix(camera.combined);
                this.batch.begin();
                context.gizmoSystem.render(this.batch, camera.zoom);
                this.batch.end();

                batch.begin();
                ScissorStack.popScissors();
            }
        }

        private void drawGrid() {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1);
            
            float zoom = camera.zoom;
            float w = camera.viewportWidth * zoom;
            float h = camera.viewportHeight * zoom;
            float x = camera.position.x;
            float y = camera.position.y;
            
            float step = 100;
            if (zoom < 0.5f) step = 50;
            if (zoom > 2f) step = 200;
            
            float startX = (float)Math.floor((x - w/2) / step) * step;
            float startY = (float)Math.floor((y - h/2) / step) * step;
            
            for (float i = startX; i < x + w/2; i += step) {
                shapeRenderer.line(i, y - h/2, i, y + h/2);
            }
            for (float i = startY; i < y + h/2; i += step) {
                shapeRenderer.line(x - w/2, i, x + w/2, i);
            }
            
            // Axis
            shapeRenderer.setColor(0.5f, 0.2f, 0.2f, 1);
            shapeRenderer.line(x - w/2, 0, x + w/2, 0); // X Axis
            shapeRenderer.setColor(0.2f, 0.5f, 0.2f, 1);
            shapeRenderer.line(0, y - h/2, 0, y + h/2); // Y Axis
            
            shapeRenderer.end();
        }
        
        private void drawEntitiesDebug(java.util.List<GObject> entities) {
            for (GObject obj : entities) {
                shapeRenderer.setColor(Color.WHITE);
                shapeRenderer.circle(obj.transform.position.x, obj.transform.position.y, 20);
                if (context.getSelection() == obj) {
                    shapeRenderer.setColor(Color.YELLOW);
                    shapeRenderer.circle(obj.transform.position.x, obj.transform.position.y, 25);
                }
                drawEntitiesDebug(obj.getChildren());
            }
        }

        @Override
        public void layout() {
            camera.viewportWidth = getWidth();
            camera.viewportHeight = getHeight();
            camera.update();
        }
    }
}
