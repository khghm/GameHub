using UnityEngine;

public class DieController : MonoBehaviour
{
    public int value;
    public Sprite[] diceSprites; // Assign sprites for dice 1-6 in Inspector
    private SpriteRenderer spriteRenderer;
    private Animator anim;

    private void Awake()
    {
        spriteRenderer = GetComponent<SpriteRenderer>();
        anim = GetComponent<Animator>();
    }

    public void SetValue(int val)
    {
        value = val;
        if (diceSprites != null && diceSprites.Length >= val)
        {
            spriteRenderer.sprite = diceSprites[val - 1];
        }
        if (anim != null)
        {
            anim.SetTrigger("Roll");
        }
    }
}
