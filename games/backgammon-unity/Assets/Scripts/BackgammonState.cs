using System.Collections.Generic;

[System.Serializable]
public enum BackgammonColor
{
    White,
    Black
}

[System.Serializable]
public class Point
{
    public int index;
    public List<BackgammonColor> checkers = new List<BackgammonColor>();
    public bool IsBlot => checkers.Count == 1;
    public bool IsBlocked => checkers.Count >= 2;
    public BackgammonColor? Owner => checkers.Count > 0 ? checkers[0] : null;

    public Point(int idx)
    {
        index = idx;
    }
}

public class BackgammonState
{
    public List<string> players;
    public string currentPlayer;
    public BackgammonColor turn = BackgammonColor.White;
    public List<Point> points;
    public int barWhite = 0;
    public int barBlack = 0;
    public int borneOffWhite = 0;
    public int borneOffBlack = 0;
    public List<int> dice = new List<int>();
    public bool diceRolled = false;
    public bool gameOver = false;

    public BackgammonState(List<string> playerIds)
    {
        players = playerIds;
        currentPlayer = playerIds[0];
        points = CreateInitialPoints();
    }

    public static List<Point> CreateInitialPoints()
    {
        var points = new List<Point>();
        for (int i = 0; i < 25; i++)
        {
            points.Add(new Point(i));
        }

        // White's checkers
        points[24].checkers.AddRange(new List<BackgammonColor> { BackgammonColor.White, BackgammonColor.White });
        points[13].checkers.AddRange(new List<BackgammonColor> { BackgammonColor.White, BackgammonColor.White, BackgammonColor.White, BackgammonColor.White, BackgammonColor.White });
        points[8].checkers.AddRange(new List<BackgammonColor> { BackgammonColor.White, BackgammonColor.White, BackgammonColor.White });
        points[6].checkers.AddRange(new List<BackgammonColor> { BackgammonColor.White, BackgammonColor.White, BackgammonColor.White, BackgammonColor.White, BackgammonColor.White });

        // Black's checkers
        points[1].checkers.AddRange(new List<BackgammonColor> { BackgammonColor.Black, BackgammonColor.Black });
        points[12].checkers.AddRange(new List<BackgammonColor> { BackgammonColor.Black, BackgammonColor.Black, BackgammonColor.Black, BackgammonColor.Black, BackgammonColor.Black });
        points[17].checkers.AddRange(new List<BackgammonColor> { BackgammonColor.Black, BackgammonColor.Black, BackgammonColor.Black });
        points[19].checkers.AddRange(new List<BackgammonColor> { BackgammonColor.Black, BackgammonColor.Black, BackgammonColor.Black, BackgammonColor.Black, BackgammonColor.Black });

        return points;
    }
}
