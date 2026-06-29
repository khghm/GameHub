using UnityEngine;
using UnityEngine.UI;
using System.Collections.Generic;

public class BackgammonGameManager : MonoBehaviour
{
    public static BackgammonGameManager Instance;

    public BackgammonState state;

    [Header("Board & Prefabs")]
    public SpriteRenderer boardRenderer;
    public GameObject whiteCheckerPrefab;
    public GameObject blackCheckerPrefab;
    public Transform[] pointSpawnPoints;
    public Transform whiteBarSpawnPoint;
    public Transform blackBarSpawnPoint;
    public Transform whiteBearOffSpawnPoint;
    public Transform blackBearOffSpawnPoint;

    [Header("Dice")]
    public GameObject dicePrefab;
    public Transform diceSpawnPoint;
    public Transform diceDiscardPoint;
    public Text die1;
    public Text die2;
    public List<GameObject> activeDice = new List<GameObject>();

    [Header("UI")]
    public Button rollDiceButton;
    public Button endTurnButton;
    public Text statusText;
    public Text whiteBorneOffText;
    public Text blackBorneOffText;

    public List<CheckerController> allCheckers = new List<CheckerController>();

    private void Awake()
    {
        if (Instance == null)
        {
            Instance = this;
        }
        else
        {
            Destroy(gameObject);
        }
    }

    public void StartNewGame(string player1Id, string player2Id)
    {
        state = new BackgammonState(new List<string> { player1Id, player2Id });
        SpawnAllCheckers();
        UpdateUI();
    }

    public void RollDice()
    {
        if (state.diceRolled) return;

        // Clear previous dice
        foreach (var die in activeDice)
        {
            Destroy(die);
        }
        activeDice.Clear();

        int d1 = Random.Range(1, 7);
        int d2 = Random.Range(1, 7);

        if (d1 == d2)
        {
            state.dice.AddRange(new List<int> { d1, d1, d1, d1 });
        }
        else
        {
            state.dice.AddRange(new List<int> { d1, d2 });
        }
        state.diceRolled = true;

        SpawnDice(d1, d2);
        UpdateUI();
    }

    private void SpawnAllCheckers()
    {
        foreach (var checker in allCheckers)
        {
            Destroy(checker.gameObject);
        }
        allCheckers.Clear();

        for (int i = 1; i < 25; i++)
        {
            var point = state.points[i];
            foreach (var color in point.checkers)
            {
                var prefab = color == BackgammonColor.White ? whiteCheckerPrefab : blackCheckerPrefab;
                var spawnPoint = pointSpawnPoints[i - 1];
                var checkerObj = Instantiate(prefab, spawnPoint.position, Quaternion.identity);
                var controller = checkerObj.GetComponent<CheckerController>();
                controller.color = color;
                controller.currentPoint = i;
                controller.isOnBar = false;
                allCheckers.Add(controller);
            }
        }
    }

    private void SpawnDice(int d1, int d2)
    {
        die1.text = d1.ToString();
        die2.text = d2.ToString();
    }

    public void HandleCheckerDrop(CheckerController checker)
    {
        // Add your move validation & execution here
    }

    private void UpdateUI()
    {
        if (state.gameOver)
        {
            statusText.text = "بازی تمام شد!";
            rollDiceButton.gameObject.SetActive(false);
            endTurnButton.gameObject.SetActive(false);
        }
        else if (state.currentPlayer != null)
        {
            statusText.text = "نوبت شماست!";
            rollDiceButton.gameObject.SetActive(!state.diceRolled);
            endTurnButton.gameObject.SetActive(state.diceRolled);
        }

        whiteBorneOffText.text = $"سفید: " + state.borneOffWhite;
        blackBorneOffText.text = $"سیاه: " + state.borneOffBlack;
    }
}
