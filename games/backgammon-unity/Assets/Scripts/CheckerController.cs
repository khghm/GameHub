using UnityEngine;
using System.Collections;

public class CheckerController : MonoBehaviour
{
    public BackgammonColor color;
    public int currentPoint;
    public bool isOnBar;
    public bool isDragging;

    private Rigidbody2D rb;
    private SpriteRenderer spriteRenderer;
    private Vector3 originalPosition;
    private Camera mainCam;

    private void Awake()
    {
        rb = GetComponent<Rigidbody2D>();
        spriteRenderer = GetComponent<SpriteRenderer>();
        mainCam = Camera.main;
    }

    public void MoveTo(Vector3 targetPosition, float duration = 0.5f)
    {
        originalPosition = transform.position;
        StartCoroutine(SmoothMove(targetPosition, duration));
    }

    private IEnumerator SmoothMove(Vector3 target, float duration)
    {
        float time = 0f;
        Vector3 startPos = transform.position;
        while (time < duration)
        {
            transform.position = Vector3.Lerp(startPos, target, time / duration);
            time += Time.deltaTime;
            yield return null;
        }
        transform.position = target;
    }

    private void OnMouseDown()
    {
        if (BackgammonGameManager.Instance.state.turn != color) return;
        if (!BackgammonGameManager.Instance.state.diceRolled) return;
        isDragging = true;
        originalPosition = transform.position;
        rb.isKinematic = true;
    }

    private void OnMouseDrag()
    {
        if (!isDragging) return;
        Vector3 mousePos = mainCam.ScreenToWorldPoint(Input.mousePosition);
        mousePos.z = 0;
        transform.position = mousePos;
    }

    private void OnMouseUp()
    {
        if (!isDragging) return;
        isDragging = false;
        rb.isKinematic = false;

        // Check if dropped on a valid point (this logic will be handled by BackgammonGameManager)
        BackgammonGameManager.Instance.HandleCheckerDrop(this);
    }
}
