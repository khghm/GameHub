using UnityEngine;

public class PointController : MonoBehaviour
{
    public int pointIndex; // 1-24
    public Color pointColor;
    public SpriteRenderer pointRenderer;

    private void Awake()
    {
        if (pointRenderer == null)
        {
            pointRenderer = GetComponent<SpriteRenderer>();
        }
    }

    public void SetColor(Color color)
    {
        pointColor = color;
        if (pointRenderer != null)
        {
            pointRenderer.color = color;
        }
    }
}
