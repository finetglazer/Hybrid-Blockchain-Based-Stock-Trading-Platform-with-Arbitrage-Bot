// import React from "react";

// const Home = () => {
//   return <p>HOME</p>;
// };

// export default Home;
import React from "react";
import "./MainBackground.css";
const wallets = [
  {
    name: "MB Bank",
    balance: "$100.000",
    icon: "ETH",
    symbol: "ETH",
  },
];

export default function Home() {
  return (
      <main className = "main-background">
        {/* <header style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 30 }}>
        <h1>Home</h1>
        <div>@Guest</div>
      </header> */}

        <section style={{ marginBottom:370 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <h1 style={{color: "#ffffff" }}>Wallets</h1>
            <a href="#" style={{ color: "#ffffff", fontWeight: "bold", textDecoration: "none" }}>View all</a>
          </div>
          <div style={{ display: "flex", gap: 20, marginTop: 15 }}>
            {wallets.map((wallet, index) => (
                <div key={index} style={{
                  background: "black",
                  padding: 20,
                  borderRadius: 15,
                  width: 250,
                  boxShadow: "0 2px 8px rgba(0,0,0,0.05)"
                }}>
                  <div style={{ display: "flex", alignItems: "center", marginBottom: 10 }}>
                    <div style={{
                      width: 40,
                      height: 40,
                      borderRadius: "50%",
                      backgroundColor: "#0000FF"
                    }}></div>
                    <div style={{ marginLeft: 10, fontWeight: "bold" }}>{wallet.name}</div>
                  </div>
                  <div style={{ fontSize: 14, color: "#555" }}>{wallet.balance}</div>
                  <div style={{ marginTop: 10, display: "flex", gap: 10 }}>
                    <button style={{
                      flex: 1,
                      padding: 10,
                      border: "none",
                      backgroundColor: "#1ca443c5",
                      color: "white",
                      borderRadius: 8
                    }}>Deposit</button>
                    <button style={{
                      flex: 1,
                      padding: 10,
                      backgroundColor: "#1ca443c5",
                      borderRadius: 8
                    }}>Withdraw</button>
                  </div>
                </div>
            ))}
          </div>
        </section>

        <section>
          <h1>Trade</h1>
          <div style={{ display: "flex", gap: 40, marginTop: 15 }}>
            {[
              { label: "Buy", icon: "+" },
              { label: "Sell", icon: "-" },
              { label: "Swap", icon: "⇄" },
            ].map((action, idx) => (
                <div key={idx} style={{ textAlign: "center" }}>
                  <div style={{
                    width: 60,
                    height: 60,
                    borderRadius: "50%",
                    backgroundColor: "#7B61FF",
                    color: "white",
                    fontSize: 30,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    margin: "auto"
                  }}>{action.icon}</div>
                  <div style={{ marginTop: 10, fontWeight: 600 }}>{action.label}</div>
                </div>
            ))}
          </div>
        </section>
      </main>
  );
}
